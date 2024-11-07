package com.example.kotlin.chat.service

import com.example.kotlin.chat.asDomainObject

import com.example.kotlin.chat.repository.MessageRepository
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer
import org.springframework.stereotype.Service

@Service
class RedisMessageService(
    private val messageRepository: MessageRepository,
    private val redisTemplate: ReactiveRedisTemplate<String, String>, // ReactiveRedisTemplate<String, String>으로 변경
    private val listenerContainer: ReactiveRedisMessageListenerContainer,
    @Qualifier("messageTopic") private val messageTopic: ChannelTopic,
    private val objectMapper: ObjectMapper // ObjectMapper를 주입
) : MessageService {

    private val logger = LoggerFactory.getLogger(RedisMessageService::class.java)

    // 각 roomId별로 개별 스트림을 관리하는 맵
    private val roomStreams: MutableMap<String, MutableSharedFlow<MessageVM>> = mutableMapOf()

    init {
        // Redis 구독 설정: 메시지가 수신되면 해당 roomId의 스트림에 emit
        GlobalScope.launch {
            listenerContainer.receive(messageTopic)
                .asFlow() // Flux를 Flow로 변환
                .collect { message ->
                    val messageString = message.message as String
                    var deserializedMessage = objectMapper.readValue(messageString, MessageVM::class.java)
                    // `sent` 필드에 9시간을 추가
                    deserializedMessage = deserializedMessage.copy(
                        sent = deserializedMessage.sent.plusSeconds(9 * 3600)
                    )
                    // roomId에 따른 스트림에 메시지를 emit
                    val roomStream = getOrCreateStream(deserializedMessage.roomId)
                    roomStream.emit(deserializedMessage)
                    logger.info("Received message for roomId ${deserializedMessage.roomId}: $deserializedMessage")
                }
        }
    }
    // roomId별로 메시지 스트림을 제공하는 메서드
    override fun stream(roomId: String): Flow<MessageVM> = getOrCreateStream(roomId).asSharedFlow()

    // 최신 메시지 가져오기
    override fun latest(roomId: String): Flow<MessageVM> {

        return TODO("Provide the return value")
    }

    // 특정 메시지 이후의 메시지 가져오기
    override fun after(messageId: Int, roomId: String): Flow<MessageVM> {

        return TODO("Provide the return value")
    }


    // 메시지 전송 및 Redis 채널 발행
    override suspend fun post(messages: Flow<MessageVM>) {
        messages.collect { message ->
            // 메시지를 영속화하고, ID가 포함된 상태를 보장
            var savedMessage = messageRepository.save(message.asDomainObject())

            // 만약 ID가 할당되지 않았다면, 다시 로드하여 ID가 포함된 객체를 얻기
            if (savedMessage.id == null) {
                savedMessage = messageRepository.findById(savedMessage.id!!)!!
            }

            val renderedMessage = message.copy(id=savedMessage.id )
            // MessageVM 객체를 JSON 문자열로 변환
            val messageString = objectMapper.writeValueAsString(renderedMessage)

            // Redis 채널에 ID가 포함된 메시지 발행
            redisTemplate.convertAndSend(messageTopic.topic, messageString).subscribe()

            logger.info("Published message for roomId ${renderedMessage.roomId}: $messageString")
        }
    }

    // 특정 roomId에 대한 스트림을 반환하고, 존재하지 않으면 생성
    private fun getOrCreateStream(roomId: String): MutableSharedFlow<MessageVM> {
        return roomStreams.getOrPut(roomId) { MutableSharedFlow() }
    }
}
