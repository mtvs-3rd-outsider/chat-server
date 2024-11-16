package com.example.kotlin.chat.service

import com.example.kotlin.chat.repository.ChatThreadRepository
import com.example.kotlin.chat.repository.Participant
import com.example.kotlin.chat.repository.ParticipantRepository
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.asFlow
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime
@Service
class RedisMessageThreadListInfoService(
    private val chatThreadRepository: ChatThreadRepository,
    private val participantRepository: ParticipantRepository,
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val listenerContainer: ReactiveRedisMessageListenerContainer,
    private val objectMapper: ObjectMapper,
    private val userStatusService: UserStatusService,
    @Qualifier("threadListInfo") private val messageTopic: ChannelTopic
) : RealtimeEventService<Map<String, RoomInfoVM>, MessageVM> {

    // 코루틴 범위를 설정하여 GlobalScope 대체
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // userId 별 RoomInfoVM을 저장하는 SharedFlow 맵
    private val threadInfoStreams: MutableMap<String, MutableSharedFlow<Map<String, RoomInfoVM>>> = mutableMapOf()

    init {
        coroutineScope.launch {
            listenerContainer.receive(messageTopic)
                .asFlow()
                .collect { message ->
                    val messageString = message.message as String
                    val roomInfo = objectMapper.readValue(messageString, RoomInfoVM::class.java)
                    val userId = roomInfo.userId

                    // 현재 사용자 스트림의 기존 데이터 가져오기
                    val userStream = threadInfoStreams.getOrPut(userId) { MutableSharedFlow(replay = 1) }
                    val currentData = userStream.replayCache.firstOrNull() ?: mapOf()

                    // 현재 방 정보가 업데이트된 정보와 다를 경우에만 방출
                    if (currentData[roomInfo.roomId] != roomInfo) {
                        // 업데이트된 방 정보로 부분 맵 생성
                        val updatedData = mapOf(roomInfo.roomId to roomInfo)

                        // 변경된 부분만 사용자 스트림에 emit
                        userStream.emit(updatedData)
                    }
                }
        }
    }


    override fun stream(userId: String): Flow<Map<String, RoomInfoVM>> {
        return threadInfoStreams.getOrPut(userId) { MutableSharedFlow(replay = 1) }.asSharedFlow()
    }

    private fun getOrCreateStream(userId: String): MutableSharedFlow<Map<String, RoomInfoVM>> {
        return threadInfoStreams.getOrPut(userId) { MutableSharedFlow(replay = 1) }
    }

    override suspend fun latest(userId: String): Flow<Map<String, RoomInfoVM>> {
        val participants = participantRepository.findByUserId(userId.toLong())

        val roomInfoMap = mutableMapOf<String, RoomInfoVM>()
        participants.collect { participant ->
            val chatThread = chatThreadRepository.findById(participant.threadId)
            if (chatThread != null) {
                val roomInfoVM = RoomInfoVM(
                    roomId = chatThread.chatRoomId.toString(),
                    userId = userId,
                    roomName = "Room ${chatThread.chatRoomId}",
                    lastMessage = chatThread.lastMessage ?: "No message",
                    lastMessageTime = chatThread.lastMessageTime ?: Instant.now(),
                    unreadMessageCount = participant.unreadMessageCount
                )
                // 수동으로 roomId를 키로 하여 roomInfoVM 추가
                roomInfoMap[roomInfoVM.roomId] = roomInfoVM
            }
        }

        // SharedFlow에 데이터를 초기화
        getOrCreateStream(userId).emit(roomInfoMap)
        return flowOf(roomInfoMap)
    }

    override suspend fun post(inboundMessages: Flow<MessageVM>) {
        inboundMessages.collect { message ->
            val roomId = message.roomId
            val newLastMessage = message.content
            val newLastMessageTime = Instant.now()

            // 채팅방 업데이트
            val chatThread = chatThreadRepository.findById(roomId.toLong())
            chatThread?.let {
                it.lastMessage = newLastMessage
                it.lastMessageTime = newLastMessageTime
                chatThreadRepository.save(it)
            }

            // 참여자 업데이트 및 상태 확인
            val participants: Flow<Participant> = participantRepository.findByThreadId(roomId.toLong())
            participants.collect { participant ->
                val userId = participant.userId.toString()

                // 온라인 상태에 따라 unreadMessageCount 업데이트
                if (!userStatusService.isUserOnline(userId, roomId)) {
                    participant.unreadMessageCount += 1 // 읽지 않은 메시지 수 증가
                } else {
                    participant.unreadMessageCount = 0 // 온라인 상태일 때 읽지 않은 메시지 초기화
                }

                // 변경 사항 영속화
                participantRepository.save(participant)

                // 업데이트된 RoomInfoVM 생성
                val updatedRoomInfo = RoomInfoVM(
                    roomId = roomId,
                    userId = userId,
                    roomName = "Room $roomId",
                    lastMessage = newLastMessage,
                    lastMessageTime = newLastMessageTime,
                    unreadMessageCount = participant.unreadMessageCount
                )

                // Redis에 발행
                val messageString = objectMapper.writeValueAsString(updatedRoomInfo)
                redisTemplate.convertAndSend(messageTopic.topic, messageString).subscribe()
            }
        }
    }
}