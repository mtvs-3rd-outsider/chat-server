package com.example.kotlin.chat.service


import com.example.kotlin.chat.repository.Participant
import com.example.kotlin.chat.repository.ParticipantRepository
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer
import org.springframework.stereotype.Service

@Service
class RedisMessageTotalUnreadCountService(
    private val participantRepository: ParticipantRepository,
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val listenerContainer: ReactiveRedisMessageListenerContainer,
    private val objectMapper: ObjectMapper,
    @Qualifier("totalUnreadMessageCount") private val messageTopic: ChannelTopic
) : RealtimeEventService<TotalUnreadMessageCountVM, MessageVM> {

    private val unreadCountStreams: MutableMap<String, MutableSharedFlow<TotalUnreadMessageCountVM>> = mutableMapOf()

    init {
        // Redis 토픽 구독 및 메시지 수신
        GlobalScope.launch {
            listenerContainer.receive(messageTopic)
                .asFlow()
                .collect { message ->
                    val messageString = message.message as String
                    val deserializedMessage = objectMapper.readValue(messageString, TotalUnreadMessageCountVM::class.java)
                    val userId = deserializedMessage.userId

                    // Redis에서 수신한 데이터를 SharedFlow에 반영
                    getOrCreateStream(userId).emit(deserializedMessage)
                }
        }
    }

    // 사용자의 읽지 않은 메시지 개수를 가져오기 위한 스트림
    override fun stream(userId: String): Flow<TotalUnreadMessageCountVM> {
        return getOrCreateStream(userId)
    }

    // 해당 userId에 대한 SharedFlow를 가져오거나 없으면 생성
    private fun getOrCreateStream(userId: String): MutableSharedFlow<TotalUnreadMessageCountVM> {
        return unreadCountStreams.getOrPut(userId) { MutableSharedFlow(replay = 1) }
    }

    // userId에 해당하는 최신 총 읽지 않은 메시지 개수를 Redis에서 조회하여 가져옴
    override suspend fun latest(userId: String): Flow<TotalUnreadMessageCountVM> {
        val participants = participantRepository.findByUserId(userId.toLong())

        val totalUnreadCount = participants
            .map { it.unreadMessageCount }
            .toList()
            .sum()

        return flow {
            emit(TotalUnreadMessageCountVM(userId = userId, unreadMessageCount = totalUnreadCount))
        }
    }

    // 메시지가 도착했을 때 Redis를 통해 읽지 않은 메시지 개수를 업데이트하는 메서드
    override suspend fun post(inboundMessages: Flow<MessageVM>) {
        inboundMessages.collect { message ->
            val roomId = message.roomId.toLong()
            val participants: Flow<Participant> = participantRepository.findByThreadId(roomId)

            participants.collect { participant ->
                val userId = participant.userId.toString()

                val currentUnreadCount = getOrCreateStream(userId).replayCache.firstOrNull()?.unreadMessageCount ?: 0
                val updatedUnreadCount = currentUnreadCount + 1

                // 업데이트된 메시지 수를 Redis에 발행
                val totalUnreadMessage = TotalUnreadMessageCountVM(userId = userId, unreadMessageCount = updatedUnreadCount)
                val messageString = objectMapper.writeValueAsString(totalUnreadMessage)
                redisTemplate.convertAndSend(messageTopic.topic, messageString).subscribe()

                // SharedFlow에 업데이트된 메시지 수를 반영
                getOrCreateStream(userId).emit(totalUnreadMessage)
            }
        }
    }
}