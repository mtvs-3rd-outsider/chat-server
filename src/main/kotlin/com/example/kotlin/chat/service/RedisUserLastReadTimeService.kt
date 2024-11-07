package com.example.kotlin.chat.service

import com.example.kotlin.chat.repository.Participant
import com.example.kotlin.chat.repository.ParticipantRepository
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.asFlow
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer
import org.springframework.stereotype.Service
import java.time.*
import java.time.format.DateTimeFormatter

@Service
@Profile("redis")
class RedisUserLastReadTimeService(
    private val userStatusService: UserStatusService,
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val listenerContainer: ReactiveRedisMessageListenerContainer,
    private val objectMapper: ObjectMapper,
    @Qualifier("userLastReadTime") private val messageTopic: ChannelTopic,
    private val participantRepository: ParticipantRepository,
) : RealtimeEventService<Map<String, UserLastReadTimeVM>, MessageVM> {

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val roomUserLastReadTimeStreams: MutableMap<String, MutableSharedFlow<Map<String, UserLastReadTimeVM>>> = mutableMapOf()

    init {
        coroutineScope.launch {
            listenerContainer.receive(messageTopic)
                .asFlow()
                .collect { message ->
                    val messageString = message.message as String
                    val userLastReadTimeVM = objectMapper.readValue(messageString, UserLastReadTimeVM::class.java)
                    val roomId = userLastReadTimeVM.roomId
                    updateRoomParticipants(roomId,  userLastReadTimeVM.lastReadTime)
                }
        }
    }


    private suspend fun updateRoomParticipants(roomId: String, newLastReadableTime: String?) {
        val participants = participantRepository.findByThreadId(roomId.toLong())
        val updatedData = mutableMapOf<String, UserLastReadTimeVM>()

        participants.collect { participant ->
            val userId = participant.userId.toString()

            // isUserOnline 상태인 참가자만 업데이트
            if (userStatusService.isUserOnline(userId, roomId)) {
                participant.lastReadTime = LocalDateTime.now()
                participantRepository.save(participant)

                val userLastReadTimeVM = UserLastReadTimeVM(
                    userId = userId,
                    lastReadTime = LocalDateTime.now().toString(),
                    roomId = roomId
                )
                updatedData[userId] = userLastReadTimeVM
            }
        }

        // 업데이트된 데이터가 있는 경우에만 emit
        if (updatedData.isNotEmpty()) {
            getOrCreateRoomStream(roomId).emit(updatedData)
        }
    }

    override fun stream(roomId: String): Flow<Map<String, UserLastReadTimeVM>> {
        return  roomUserLastReadTimeStreams.getOrPut(roomId){ MutableSharedFlow(replay = 1) }.asSharedFlow()
    }

    override suspend fun latest(id: String): Flow<Map<String, UserLastReadTimeVM>> = flow {
        val participants = participantRepository.findByThreadId(id.toLong())
        val roomInfo = mutableMapOf<String, UserLastReadTimeVM>()

        participants.collect { participant ->
            val userId = participant.userId.toString()
            println(participant.lastReadTime)

            // 한국 시간대로 변환
            val lastReadTimeString = participant.lastReadTime
                ?.atZone(ZoneId.systemDefault())
                ?.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

            roomInfo[userId] = UserLastReadTimeVM(
                userId = userId,
                lastReadTime = lastReadTimeString ?: Instant.now().toString(),
                roomId = id
            )
            println("lastReadTimeString: " + roomInfo[userId]?.lastReadTime)
        }

        // 최종 `roomInfo`를 emit 한 번만 수행
        emit(roomInfo)
    }


    override suspend fun post(inboundMessages: Flow<MessageVM>) {
        inboundMessages.collect { message ->
            val roomId = message.roomId
            val newLastReadableTime = message.sent

            val userLastReadTime = UserLastReadTimeVM(
                userId = message.user.id,
                lastReadTime = newLastReadableTime.toString(),
                roomId = roomId
            )
            val messageString = objectMapper.writeValueAsString(userLastReadTime)
            redisTemplate.convertAndSend(messageTopic.topic, messageString).subscribe()
        }
    }

    private fun getOrCreateRoomStream(roomId: String): MutableSharedFlow<Map<String, UserLastReadTimeVM>> {
        return roomUserLastReadTimeStreams.getOrPut(roomId){ MutableSharedFlow(replay = 1) }
    }
}