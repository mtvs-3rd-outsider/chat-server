package com.example.kotlin.chat.service

import NoRedisUserStatusService
import com.example.kotlin.chat.repository.Participant
import com.example.kotlin.chat.repository.ParticipantRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneId

@Service
class UserLastReadTimeService(
    private val userStatusService: UserStatusService // UserStatusService를 주입
    , private val participantRepository: ParticipantRepository
) : RealtimeEventService<List<UserLastReadTimeVM>, MessageVM> {

    // 각 방(roomId)에 대한 마지막 읽은 시간을 저장하는 맵, roomId가 키로 사용됨
    private val roomLastReadTimeStreams: MutableMap<String, MutableSharedFlow<List<UserLastReadTimeVM>>> = mutableMapOf()

    // 특정 roomId에 대한 마지막 읽은 시간을 관리하는 Flow 스트림을 가져옴
    override fun stream(roomId: String): Flow<List<UserLastReadTimeVM>> {
        return getOrCreateStream(roomId)
    }

    override suspend fun latest(id: String): Flow<List<UserLastReadTimeVM>> {
        TODO("Not yet implemented")
    }

    // 특정 roomId에 대한 SharedFlow를 가져오거나 없으면 생성
    private fun getOrCreateStream(roomId: String): MutableSharedFlow<List<UserLastReadTimeVM>> {
        return roomLastReadTimeStreams.getOrPut(roomId) { MutableSharedFlow(replay = 1) }
    }

    // 메시지 스트림을 받아서 각 메시지에 대해 처리하는 메서드
    override suspend fun post(inboundMessages: Flow<MessageVM>) {
        inboundMessages.collect { message ->
            val userId = message.user.id
            val roomId = message.roomId
            val newLastReadableTime = message.sent // 메시지의 타임스탬프를 마지막 읽은 시간으로 사용

            // SharedFlow에서 현재 상태를 가져옴 (replayCache 사용)
            val currentList = getOrCreateStream(roomId).replayCache.firstOrNull() ?: listOf()

            // 각 사용자에 대해 온라인 상태를 체크하고, 필요한 경우 lastReadTime과 unreadMessageCount를 업데이트
            val updatedList = currentList.map { readTime ->
                val participant = participantRepository.findByUserIdAndThreadId(userId.toLong(), roomId.toLong())

                if (participant != null) {
                    if (userStatusService.isUserOnline(readTime.userId, roomId)) {
                        // 사용자가 온라인 상태일 경우 lastReadTime 업데이트, unreadMessageCount는 유지
                        participant.lastReadTime = LocalDateTime.ofInstant(newLastReadableTime, ZoneId.systemDefault())
                        participantRepository.save(participant) // 변경 사항 영속화
                        readTime.copy(lastReadableTime = newLastReadableTime)
                    } else {
                        // 사용자가 오프라인 상태일 경우 unreadMessageCount 증가, lastReadTime은 유지
                        participant.unreadMessageCount += 1
                        participantRepository.save(participant) // 변경 사항 영속화
                        readTime // 마지막 읽은 시간은 그대로 유지
                    }
                } else {
                    readTime // participant가 없을 경우 그대로 유지
                }
            }.toMutableList()

            // 해당 userId의 정보가 없다면 새로 추가하고 영속화
            if (updatedList.none { it.userId == userId }) {
                val newParticipant = Participant(
                    userId = userId.toLong(),  // userId를 Long으로 변환
                    threadId = roomId.toLong(),
                    isOnline = userStatusService.isUserOnline(userId, roomId), // userId와 roomId로 상태 조회
                    lastReadTime = if (userStatusService.isUserOnline(userId, roomId)) {
                        LocalDateTime.ofInstant(newLastReadableTime, ZoneId.systemDefault()) // Instant를 LocalDateTime으로 변환
                    } else null,
                    unreadMessageCount = if (!userStatusService.isUserOnline(userId, roomId)) 1 else 0
                )
                participantRepository.save(newParticipant) // 새로운 participant 영속화

                updatedList.add(
                    UserLastReadTimeVM(
                        userId = userId,
                        lastReadableTime = newLastReadableTime,
                        roomId = roomId,
                    )
                )
            }

            // SharedFlow에 업데이트된 리스트를 다시 방출
            getOrCreateStream(roomId).emit(updatedList)
        }
    }

    // 이하 기존 코드 유지
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
