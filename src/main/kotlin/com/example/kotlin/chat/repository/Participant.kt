package com.example.kotlin.chat.repository

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.time.LocalDateTime

@Table("participants")
data class Participant(
    @Id
    @Column("id")
    val id: Long? = null,  // 기본 키

    @Column("chat_room_id")
    val threadId: Long,  // 채팅방과의 관계를 나타내는 외래 키

    @Column("user_id")
    val userId: Long,  // 사용자 ID

    @Column("is_online")
    var isOnline: Boolean = false,  // 사용자의 온라인 여부

    @Column("last_read_time")
    var lastReadTime: Instant? = Instant.now(),  // 사용자의 마지막 읽은 시간

    @Column("unread_message_count")
    var unreadMessageCount: Int = 0  // 읽지 않은 메시지 수
)