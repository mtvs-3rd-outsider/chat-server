package com.example.kotlin.chat.repository

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.time.LocalDateTime

@Table("chat_threads")
data class ChatThread(
    @Id
    @Column("chat_room_id")
    val chatRoomId: Long? = null,  // 채팅방을 구분하는 고유 ID

    @Column("created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column("last_message")
    var lastMessage: String? = null,  // 마지막 메시지 내용을 저장하는 필드

    @Column("last_message_time")
    var lastMessageTime: Instant? = null,  // 마지막 메시지가 전송된 시간을 저장하는 필드

    @Column("is_group_thread")
    val isGroupThread: Boolean = false // 그룹 채팅 여부
)