package com.example.kotlin.chat.service.dto

import java.time.Instant

/** 사용자 전체 정보를 포함하는 대신, 간단히 ID만 포함하는 DTO. 클라이언트는 이 ID를 사용해 별도로 사용자 정보를 조회해야 합니다. */
data class SimpleParticipantDTO(val userId: String)

data class ChatThreadDTO(
        val chatRoomId: Long,
        val createdAt: Instant,
        val lastMessage: String?,
        val lastMessageTime: Instant?,
        val isGroupThread: Boolean,
        val participants: List<SimpleParticipantDTO>
)

data class CreateChatThreadDTO(
        val isGroupThread: Boolean?,
        val participantIds: List<String> = emptyList()
)

data class CreateChatThreadResponse(val chatRoomId: String)
