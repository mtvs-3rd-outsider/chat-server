package com.example.kotlin.chat.service

import com.example.kotlin.chat.repository.ChatThreadRepository
import com.example.kotlin.chat.repository.Participant
import com.example.kotlin.chat.repository.ParticipantRepository
import kotlinx.coroutines.flow.*
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.time.LocalDateTime
@Service
class MessageThreadListInfoService(
    private val chatThreadRepository: ChatThreadRepository,
    private val participantRepository: ParticipantRepository,
) : RealtimeEventService<List<RoomInfoVM>,MessageVM> {

    private val threadInfoStreams: MutableMap<String, MutableSharedFlow<List<RoomInfoVM>>> = mutableMapOf()

    override fun stream(id: String): Flow<List<RoomInfoVM>> {
        return getOrCreateStream(id)
    }

    private fun getOrCreateStream(userId: String): MutableSharedFlow<List<RoomInfoVM>> {
        return threadInfoStreams.getOrPut(userId) { MutableSharedFlow(replay = 1) }
    }

    override suspend fun latest(id: String): Flow<List<RoomInfoVM>> {
        // 사용자가 속한 모든 방을 조회
        val participants = participantRepository.findByUserId(id.toLong())
        // Flow를 List로 변환하여 Flow<List<RoomInfoVM>>로 반환
        return flow {
            val roomInfoList = participants.map { participant ->
                val chatThread = chatThreadRepository.findById(participant.threadId) // 방 정보 조회
                RoomInfoVM(
                    roomId = chatThread?.chatRoomId.toString(),
                    roomName = "Room ${chatThread?.chatRoomId}", // 방 이름
                    lastMessage = chatThread?.lastMessage ?: "No message",
                    lastMessageTime = chatThread?.lastMessageTime ?: LocalDateTime.now(),
                    unreadMessageCount = participant.unreadMessageCount
                )
            }.toList() // Flow<RoomInfoVM>를 List<RoomInfoVM>로 변환
            emit(roomInfoList) // List<RoomInfoVM>를 Flow로 방출
        }
    }
    // 메시지가 도착했을 때 DB를 참고하지 않고 SharedFlow에서 최신 방 정보를 업데이트하는 메서드
    override suspend fun post(inboundMessages: Flow<MessageVM>) {
        inboundMessages.collect { message ->
            val roomId = message.roomId.toLong()
            // 채팅방 참여자(userId)에게 방 정보를 방출
            val participants: Flow<Participant> = participantRepository.findByThreadId(roomId) // Flow로 가정
            participants.collect { participant ->
                val userId = participant.userId.toString()

                // 해당 userId의 SharedFlow에서 현재 상태를 가져옴
                val currentRoomInfoList = getOrCreateStream(userId).replayCache.firstOrNull() ?: listOf()

                // 현재 상태에서 해당 방 정보만 업데이트
                val updatedRoomInfoList = currentRoomInfoList.map { roomInfo ->
                    if (roomInfo.roomId == roomId.toString()) {
                        // 해당 방에 대한 정보를 부분 업데이트
                        roomInfo.copy(
                            lastMessage = message.content,
                            lastMessageTime = LocalDateTime.now(),
                            unreadMessageCount = roomInfo.unreadMessageCount + 1 // 읽지 않은 메시지 수 증가
                        )
                    } else {
                        roomInfo // 다른 방 정보는 그대로 유지
                    }
                }
                // 해당 userId의 SharedFlow에 업데이트된 방 정보를 다시 방출
                getOrCreateStream(userId).emit(updatedRoomInfoList)
            }
        }
    }
}
