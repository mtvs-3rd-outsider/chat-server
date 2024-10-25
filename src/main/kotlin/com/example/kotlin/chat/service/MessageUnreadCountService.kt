package com.example.kotlin.chat.service

import com.example.kotlin.chat.repository.Participant
import com.example.kotlin.chat.repository.ParticipantRepository
import kotlinx.coroutines.flow.*
import org.springframework.stereotype.Service

@Service
class MessageUnreadCountService(
    private val participantRepository: ParticipantRepository
) {
    // 각 사용자에 대한 총 읽지 않은 메시지 개수를 관리하는 SharedFlow
    private val unreadCountStreams: MutableMap<String, MutableSharedFlow<TotalUnreadMessageCountVM>> = mutableMapOf()

    // 사용자의 읽지 않은 메시지 개수를 가져오기 위한 스트림
    fun stream(userId: String): Flow<TotalUnreadMessageCountVM> {
        return getOrCreateStream(userId)
    }

    // 해당 userId에 대한 SharedFlow를 가져오거나 없으면 생성
    private fun getOrCreateStream(userId: String): MutableSharedFlow<TotalUnreadMessageCountVM> {
        return unreadCountStreams.getOrPut(userId) { MutableSharedFlow(replay = 1) }
    }

    // userId에 해당하는 최신 총 읽지 않은 메시지 개수를 가져오는 메서드
    suspend fun latest(userId: String): Flow<TotalUnreadMessageCountVM> {
        // 사용자가 속한 모든 방을 조회
        val participants = participantRepository.findByUserId(userId.toLong())

        // 총 읽지 않은 메시지 수 계산
        val totalUnreadCount = participants
            .map { it.unreadMessageCount }
            .toList() // Flow를 리스트로 변환
            .sum() // 각 참여자의 읽지 않은 메시지 수 합산

        return flow {
            emit(TotalUnreadMessageCountVM(unreadMessageCount = totalUnreadCount)) // 총 읽지 않은 메시지 개수를 방출
        }
    }

    // 메시지가 도착했을 때 SharedFlow에서 읽지 않은 메시지 개수를 업데이트하는 메서드
    suspend fun post(inboundMessages: Flow<MessageVM>) {
        inboundMessages.collect { message ->
            val roomId = message.roomId.toLong()

            // 채팅방의 모든 참여자를 가져옴
            val participants: Flow<Participant> = participantRepository.findByThreadId(roomId)
            participants.collect { participant ->
                val userId = participant.userId.toString()

                // 현재 SharedFlow에서 총 읽지 않은 메시지 수를 가져옴
                val currentUnreadCount = getOrCreateStream(userId).replayCache.firstOrNull()?.unreadMessageCount ?: 0

                // 해당 방의 unreadMessageCount 증가
                val updatedUnreadCount = currentUnreadCount + 1

                // 업데이트된 총 읽지 않은 메시지 수 방출
                getOrCreateStream(userId).emit(TotalUnreadMessageCountVM(unreadMessageCount = updatedUnreadCount))
            }
        }
    }
}