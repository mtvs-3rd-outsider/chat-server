package com.example.kotlin.chat.repository

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

@Table("MESSAGES")
data class Message(
    val content: String,
    @Column("content_type")
    val contentTypeStr: String,
    val sent: Instant,
    @Column("user_id")
    val userId: String,
    @Column("room_id")
    val roomId: String,
    @Id var id: Int?,
    @Column("reply_to_message_id")
    val replyToMessageId: String?,
    @Column("reply_content")
    val replyContent: String?, // 답글 내용 추가
    @Column("media_url")
    val mediaUrl: String? // 미디어 URL 추가



) {
    val contentType: ContentType
        get() = ContentType.valueOf(contentTypeStr.uppercase(Locale.getDefault()))
}

enum class ContentType {
    PLAIN, MARKDOWN, IMAGE, VIDEO, FEED
}

