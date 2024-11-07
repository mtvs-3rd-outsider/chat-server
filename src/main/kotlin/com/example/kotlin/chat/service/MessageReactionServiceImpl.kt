package com.example.kotlin.chat.service

import com.example.kotlin.chat.toDomainObject
import com.example.kotlin.chat.repository.MessageReactionRepository
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer
import org.springframework.stereotype.Service

@Service
class MessageReactionServiceImpl(
    private val messageReactionRepository: MessageReactionRepository,
    private val redisTemplate: ReactiveRedisTemplate<String, String>, // Redis 통합
    private val listenerContainer: ReactiveRedisMessageListenerContainer,
    @Qualifier("reactionTopic") private val reactionTopic: ChannelTopic, // Redis 채널
    private val objectMapper: ObjectMapper
) : MessageReactionService {

    private val logger = LoggerFactory.getLogger(MessageReactionServiceImpl::class.java)
    private val roomStreams: MutableMap<String, MutableSharedFlow<MessageReactionVM>> = mutableMapOf()

    init {
        // Redis 메시지 리스너 설정: 수신된 메시지를 해당 roomId 스트림으로 emit
        CoroutineScope(Dispatchers.IO).launch {
            listenerContainer.receive(reactionTopic)
                .asFlow()
                .collect { message ->
                    val messageString = message.message as String
                    val reaction = objectMapper.readValue(messageString, MessageReactionVM::class.java)
                    getOrCreateStream(reaction.roomId).emit(reaction)
                    logger.info("Received reaction for roomId ${reaction.roomId}: $reaction")
                }
        }
    }
    override suspend fun post(reactions: Flow<MessageReactionVM>) {
        reactions.onEach { reaction ->
            val roomId = reaction.roomId

            // 기존 반응을 확인
            val existingReaction = messageReactionRepository.findReactionByMessageIdAndUserIdAndReactionTypeAndRoomId(
                reaction.messageId, reaction.userId, reaction.reactionType, roomId
            )

            // 상태 변경 및 영속화 처리
            val modifiedReaction = when (reaction.actionType) {
                "plus" -> {
                    if (existingReaction != null) {
                        // 이미 반응이 있는 경우 "none" 상태로 전환
                        reaction.copy(actionType = "none")
                    } else {
                        // 반응이 없는 경우 새로운 "plus" 상태로 유지하여 저장
                        messageReactionRepository.save(reaction.toDomainObject())
                        reaction.copy(actionType = "plus")
                    }
                }
                "minus" -> {
                    if (existingReaction != null) {

                        // 반응이 없는 경우 새로운 "minus" 상태로 유지하여 저장
                        messageReactionRepository.deleteByMessageIdAndUserIdAndReactionTypeAndRoomId(reaction.messageId,reaction.userId, reaction.reactionType, roomId)
                        reaction.copy(actionType = "minus")
                    } else {
                        reaction.copy(actionType = "none")
                    }
                }
                else -> reaction // "none" 상태일 때 그대로 유지
            }

            // 변경된 상태를 Redis 채널에 발행
            val reactionString = objectMapper.writeValueAsString(modifiedReaction)
            redisTemplate.convertAndSend(reactionTopic.topic, reactionString).subscribe()
            logger.info("Published modified reaction for roomId $roomId: $reactionString")

        }.collect()
    }

    private fun getOrCreateStream(roomId: String): MutableSharedFlow<MessageReactionVM> {
        return roomStreams.getOrPut(roomId) { MutableSharedFlow() }
    }

    override fun stream(roomId: String): Flow<MessageReactionVM> = getOrCreateStream(roomId)
}
