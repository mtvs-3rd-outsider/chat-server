package com.example.kotlin.chat.repository

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.*

@Table("MESSAGES")
data class Message(
    val content: String,
    @Column("content_type")
    val contentTypeStr: String, // 데이터베이스에서 읽어온 문자열 값
    val sent: Instant,
    val username: String,
    val userAvatarImageLink: String,
    @Column("room_id")
    val roomId: Int,
    @Id var id: Int?
) {
    val contentType: ContentType
        get() = ContentType.valueOf(contentTypeStr.uppercase(Locale.getDefault())) // 문자열을 enum으로 변환
}

enum class ContentType {
    PLAIN, MARKDOWN
}
