package com.example.kotlin.chat.service

import com.example.kotlin.chat.repository.ChatThreadRepository
import com.example.kotlin.chat.repository.Participant
import com.example.kotlin.chat.repository.ParticipantRepository
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
@Profile("redis")
class RedisMessageThreadListInfoService(
    private val chatThreadRepository: ChatThreadRepository,
    private val participantRepository: ParticipantRepository,
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val listenerContainer: ReactiveRedisMessageListenerContainer,
    private val objectMapper: ObjectMapper,
    @Qualifier("threadListInfo") private val messageTopic: ChannelTopic // Redis 채널 토픽
) : RealtimeEventService<List<RoomInfoVM>, MessageVM> {

    private val threadInfoStreams: MutableMap<String, MutableSharedFlow<List<RoomInfoVM>>> = mutableMapOf()

    init {
        // Redis 토픽 구독 및 메시지 수신
        GlobalScope.launch {
            listenerContainer.receive(messageTopic)
                .asFlow()
                .collect { message ->
                    val messageString = message.message as String
                    val deserializedMessage = objectMapper.readValue(messageString, List::class.java) as List<RoomInfoVM>
                    val userId = deserializedMessage.firstOrNull()?.roomId ?: return@collect // 방 정보가 없으면 무시

                    // Redis에서 수신한 데이터를 SharedFlow에 반영
                    getOrCreateStream(userId).emit(deserializedMessage)
                }
        }
    }

    // 사용자의 방 정보 스트림을 제공
    override fun stream(userId: String): Flow<List<RoomInfoVM>> {
        return getOrCreateStream(userId)
    }

    // userId에 대한 SharedFlow 생성 또는 반환
    private fun getOrCreateStream(userId: String): MutableSharedFlow<List<RoomInfoVM>> {
        return threadInfoStreams.getOrPut(userId) { MutableSharedFlow(replay = 1) }
    }

    // 사용자가 속한 모든 방 정보를 Redis에서 최신 상태로 조회
    override suspend fun latest(id: String): Flow<List<RoomInfoVM>> {
        val participants = participantRepository.findByUserId(id.toLong())
        return flow {
            val roomInfoList = participants.map { participant ->
                val chatThread = chatThreadRepository.findById(participant.threadId) // 방 정보 조회
                RoomInfoVM(
                    roomId = chatThread?.chatRoomId.toString(),
                    roomName = "Room ${chatThread?.chatRoomId}",
                    lastMessage = chatThread?.lastMessage ?: "No message",
                    lastMessageTime = chatThread?.lastMessageTime ?: LocalDateTime.now(),
                    unreadMessageCount = participant.unreadMessageCount
                )
            }.toList()
            emit(roomInfoList)
        }
    }

    // 새 메시지가 수신되었을 때 Redis에 방 정보를 업데이트하고 발행
    override suspend fun post(inboundMessages: Flow<MessageVM>) {
        inboundMessages.collect { message ->
            val roomId = message.roomId.toLong()

            // 각 참여자에게 방 정보를 업데이트
            val participants: Flow<Participant> = participantRepository.findByThreadId(roomId)
            participants.collect { participant ->
                val userId = participant.userId.toString()

                // 현재 SharedFlow의 방 정보를 가져옴
                val currentRoomInfoList = getOrCreateStream(userId).replayCache.firstOrNull() ?: listOf()

                // 방 정보 업데이트
                val updatedRoomInfoList = currentRoomInfoList.map { roomInfo ->
                    if (roomInfo.roomId == roomId.toString()) {
                        roomInfo.copy(
                            lastMessage = message.content,
                            lastMessageTime = LocalDateTime.now(),
                            unreadMessageCount = roomInfo.unreadMessageCount + 1
                        )
                    } else {
                        roomInfo
                    }
                }

                // JSON 직렬화 후 Redis에 방 정보 발행
                val messageString = objectMapper.writeValueAsString(updatedRoomInfoList)
                redisTemplate.convertAndSend(messageTopic.topic, messageString).subscribe()

                // SharedFlow에 업데이트된 방 정보를 반영
                getOrCreateStream(userId).emit(updatedRoomInfoList)
            }
        }
    }
}
