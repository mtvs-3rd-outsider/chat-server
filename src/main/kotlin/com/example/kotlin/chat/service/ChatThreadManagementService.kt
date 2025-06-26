package com.example.kotlin.chat.service

import com.example.kotlin.chat.repository.ChatThread
import com.example.kotlin.chat.repository.ChatThreadRepository
import com.example.kotlin.chat.repository.Participant
import com.example.kotlin.chat.repository.ParticipantRepository
// DTOs are now in ViewModel.kt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.time.LocalDateTime
import kotlinx.coroutines.flow.transform
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ChatThreadManagementService(
        private val chatThreadRepository: ChatThreadRepository,
        private val participantRepository: ParticipantRepository,
) {

    @Transactional
    suspend fun createOrGetThread(
            dto: CreateChatThreadDTO,
            currentUserId: String
    ): CreateChatThreadResponse {
        // 1:1 채팅인 경우 기존 스레드 확인
        if (dto.isGroupThread == false && dto.participantIds.size == 2) {
            val otherUserId = dto.participantIds.first { it != currentUserId }

            val existingThread = chatThreadRepository.findOneToOneChatByUserIds(currentUserId.toLong(), otherUserId.toLong())
            if (existingThread != null) {
                // 방이 존재하면 해당 방 ID를 반환
                return CreateChatThreadResponse(chatRoomId = existingThread.chatRoomId.toString())
            }
        }

        // 새 스레드 생성
        val newThread =
                ChatThread(isGroupThread = dto.isGroupThread ?: (dto.participantIds.size > 2))
        val savedThread = chatThreadRepository.save(newThread)
        val threadId =
                savedThread.chatRoomId ?: throw IllegalStateException("Failed to save chat thread")

        // 참여자 추가
        val uniqueParticipantIds = (dto.participantIds + currentUserId).toSet()
        uniqueParticipantIds.forEach { userId ->
            val participant = Participant(threadId = threadId, userId = userId.toLong())
            participantRepository.save(participant)
        }

        return CreateChatThreadResponse(chatRoomId = threadId.toString())
    }

    suspend fun getThreadsByUserId(userId: String): Flow<ChatThreadDTO> {
        return flow {
            val participants = participantRepository.findByUserId(userId.toLong())
            participants.collect { participant ->
                val thread = chatThreadRepository.findById(participant.threadId)
                if (thread != null) {
                    val allParticipants = participantRepository
                        .findByThreadId(thread.chatRoomId!!)
                        .map { p -> SimpleParticipantDTO(p.userId.toString()) }
                        .toList()

                    emit(
                        ChatThreadDTO(
                            chatRoomId = thread.chatRoomId,
                            createdAt = thread.createdAt,
                            lastMessage = thread.lastMessage,
                            lastMessageTime = thread.lastMessageTime,
                            isGroupThread = thread.isGroupThread,
                            participants = allParticipants
                        )
                    )
                }
            }
        }
    }

    suspend fun getThreadDetails(chatRoomId: Long): ChatThreadDTO? {
        val thread = chatThreadRepository.findById(chatRoomId) ?: return null

        val participants =
                participantRepository
                        .findByThreadId(thread.chatRoomId!!)
                        .map { participant -> SimpleParticipantDTO(participant.userId.toString()) }
                        .toList()

        return ChatThreadDTO(
                chatRoomId = thread.chatRoomId,
                createdAt = thread.createdAt,
                lastMessage = thread.lastMessage,
                lastMessageTime = thread.lastMessageTime,
                isGroupThread = thread.isGroupThread,
                participants = participants
        )
    }

    @Transactional
    suspend fun deactivateParticipant(chatRoomId: Long, userId: String) {
        val participant = participantRepository.findByUserIdAndThreadId(userId.toLong(), chatRoomId)
        participant?.let {
            it.isOnline = false
            it.lastReadTime = Instant.now()
            participantRepository.save(it)
        }
    }
}
