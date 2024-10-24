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
    @Column("user_id")
    val userId: String,
    val userAvatarImageLink: String,
    @Column("room_id")
    val roomId: String,
    @Id var id: Int?,
    @Column("reply_to_message_id")
    val replyToMessageId: Int?, // 원본 메시지에 대한 참조
    // 반응 리스트 추가
) {
    val contentType: ContentType
        get() = ContentType.valueOf(contentTypeStr.uppercase(Locale.getDefault())) // 문자열을 enum으로 변환
}

enum class ContentType {
    PLAIN, MARKDOWN , IMAGE, VIDEO , FEED
}
@Table("MESSAGE_REACTIONS")
data class MessageReaction(
    @Id val id: Int?,
    @Column("message_id")
    val messageId: Int,
    @Column("user_id")
    val userId: String,
    @Column("reaction_type")
    val reactionType: String // 예: "like", "heart", "thumbs_up"
)