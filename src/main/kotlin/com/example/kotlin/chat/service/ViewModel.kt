package com.example.kotlin.chat.service

import java.time.Instant
import java.time.LocalDateTime

data class MessageVM(
    val content: String,
    val user: UserVM,
    val sent: Instant,
    val roomId: String,  // roomId 추가
    val id: Int? = null,
    val replyToMessageId: String? = null,
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
    val reactionType: String, // 예: "like", "heart", "thumbs_up"
    val roomId: String,
    val isPlus: Boolean
)

data class RoomInfoVM(
    val roomId: String,
    val roomName: String,
    val lastMessage: String,
    val lastMessageTime: LocalDateTime,
    val unreadMessageCount: Int  // 읽지 않은 메시지 개수
)
data class TotalUnreadMessageCountVM(
    val userId: String, // 추가한 필드
    val unreadMessageCount: Int  // 읽지 않은 메시지 개수
)
data class UserLastReadTimeVM(
    val userId: String,
    val lastReadableTime: Instant,
    val roomId: String,
)


data class UserStatusVM(
    val userId: String,
    val isOnline: Boolean
)

data class UserStatusUpdateVM(
    val userId: String,
    val isOnline: Boolean
)