package com.example.kotlin.chat.service

import com.example.kotlin.chat.repository.Message
import com.example.kotlin.chat.repository.MessageReaction
import java.net.URL
import java.time.Instant

data class MessageVM(
    val content: String,
    val user: UserVM,
    val sent: Instant,
    val roomId: String,  // roomId 추가
    val id: Int? = null,
    val replyToMessageId: Int? = null,
    val contentType: String
)

data class UserVM(
    val name: String,
    val avatarImageLink: String?,
    val id:String

)

data class MessageReactionVM(
    val id: Int?,
    val messageId: Int,
    val userId: String,
    val reactionType: String // 예: "like", "heart", "thumbs_up"
)

data class MessageWithReactionVM(
    val id: Int?,
    val content: String,
    val sent: String,
    val roomId: String,
    val reactions: List<MessageReactionVM>
)
data class MessageWithReaction(
    val message: Message,
    val reaction: MessageReaction? // LEFT JOIN이라서 반응이 없을 수도 있으므로 null 가능성 처리
)
