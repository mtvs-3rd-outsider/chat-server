package com.example.kotlin.chat.service

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer
import org.springframework.stereotype.Service



@Service
@Profile("redis")
class RedisUserLastReadTimeService(
    private val userStatusService: UserStatusService, // UserStatusService를 주입하여 온라인 상태 확인
    private val redisTemplate: ReactiveRedisTemplate<String, String>, // Redis와 상호작용하는 ReactiveRedisTemplate
    private val listenerContainer: ReactiveRedisMessageListenerContainer, // Redis 구독을 위한 ReactiveRedisMessageListenerContainer
    private val objectMapper: ObjectMapper, // JSON 직렬화를 위한 ObjectMapper
    @Qualifier("userLastReadTime") private val messageTopic: ChannelTopic // 구독할 Redis 토픽
) : RealtimeEventService<List<UserLastReadTimeVM>, MessageVM> {

    // 각 roomId에 대한 마지막 읽은 시간을 관리하는 MutableSharedFlow 저장소
    private val roomLastReadTimeStreams: MutableMap<String, MutableSharedFlow<List<UserLastReadTimeVM>>> = mutableMapOf()

    init {
        // Redis 토픽 구독 및 메시지 수신 처리
        GlobalScope.launch {
            listenerContainer.receive(messageTopic)
                .asFlow()
                .collect { message ->
                    val messageString = message.message as String
                    val deserializedMessage = objectMapper.readValue(messageString, List::class.java) as List<UserLastReadTimeVM>

                    // roomId에 해당하는 스트림에 반영
                    if (deserializedMessage.isNotEmpty()) {
                        val roomId = deserializedMessage[0].roomId
                        val roomStream = getOrCreateStream(roomId)
                        roomStream.emit(deserializedMessage)
                    }
                }
        }
    }

    // roomId에 따른 마지막 읽은 시간 스트림 제공
    override fun stream(roomId: String): Flow<List<UserLastReadTimeVM>> {
        return getOrCreateStream(roomId)
    }

    override suspend fun latest(id: String): Flow<List<UserLastReadTimeVM>> {
        TODO("Not yet implemented")
    }

    // 새로운 메시지 수신 시 Redis에 발행하고 업데이트
    override suspend fun post(inboundMessages: Flow<MessageVM>) {
        inboundMessages.collect { message ->
            val userId = message.user.id
            val roomId = message.roomId
            val newLastReadableTime = message.sent

            val currentList = getOrCreateStream(roomId).replayCache.firstOrNull() ?: listOf()
            val updatedList = currentList.map { readTime ->
                if (userStatusService.isUserOnline(readTime.userId, roomId)) {
                    readTime.copy(lastReadableTime = newLastReadableTime)
                } else {
                    readTime
                }
            }.toMutableList()

            // userId가 리스트에 없으면 새로 추가
            if (updatedList.none { it.userId == userId }) {
                updatedList.add(
                    UserLastReadTimeVM(
                        userId = userId,
                        lastReadableTime = newLastReadableTime,
                        roomId = roomId
                    )
                )
            }

            // JSON으로 직렬화 후 Redis에 발행
            val messageString = objectMapper.writeValueAsString(updatedList)
            redisTemplate.convertAndSend(messageTopic.topic, messageString).subscribe()

            // SharedFlow에 업데이트된 리스트 방출
            getOrCreateStream(roomId).emit(updatedList)
        }
    }

    // 특정 roomId에 대한 SharedFlow 스트림을 가져오거나 없으면 생성
    private fun getOrCreateStream(roomId: String): MutableSharedFlow<List<UserLastReadTimeVM>> {
        return roomLastReadTimeStreams.getOrPut(roomId) { MutableSharedFlow(replay = 1) }
    }

    // 특정 userId와 roomId에 대한 마지막 읽은 시간을 조회
    suspend fun getLastReadTime(userId: String, roomId: String): UserLastReadTimeVM? {
        val currentList = getOrCreateStream(roomId).replayCache.firstOrNull() ?: listOf()
        return currentList.firstOrNull { it.userId == userId }
    }

    // 특정 roomId에 대한 모든 읽은 시간 정보를 반환
    suspend fun getAllLastReadTimes(roomId: String): List<UserLastReadTimeVM> {
        return getOrCreateStream(roomId).replayCache.firstOrNull() ?: listOf()
    }
}
