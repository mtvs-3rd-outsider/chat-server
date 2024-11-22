package com.example.kotlin.chat.service

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.Instant
import java.time.LocalDateTime



data class MessageVM(
    val content: String,
    val user: UserVM,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX", timezone = "UTC")
    val sent: Instant,
    val roomId: String,
    val id: Int?,
    val replyToMessageId: String?,
    val replyContent: String?,  // 답글 내용 추가
    val contentType: String,
    val mediaUrl: String?,  // 미디어 URL 추가
//    val reactions: List<MessageReactionDTO> = listOf()  // 반응 목록 추가
)

data class MessageReactionDTO(
    val reactionType: String, // 예: "like", "heart"
    val userId: String, // 반응한 사용자의 ID
    val count: Int // 반응의 개수
)


data class UserVM(
    val name: String,
    val userName: String,
    val avatarImageLink: String?,
    val id:String

)

data class MessageReactionVM(
    val id: Int?,
    val messageId: Int,
    val userId: String,
    val reactionType: String, // 예: "like", "heart", "thumbs_up"
    val roomId: String,
    val actionType: String, // "plus", "minus", "none" 상태 중 하나
    val displayName: String,
    val userName: String,
    val userImg: String?
)

data class RoomInfoVM(
    val userId: String,
    val roomId: String,
    val roomName: String,
    val lastMessage: String,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX", timezone = "Asia/Seoul")
    val lastMessageTime: Instant,
    val unreadMessageCount: Int  // 읽지 않은 메시지 개수
)
data class TotalUnreadMessageCountVM(
    val userId: String, // 추가한 필드
    val roomId: String, // 추가한 필드
    val unreadMessageCount: Int  // 읽지 않은 메시지 개수
)
data class UserLastReadTimeVM(
    val userId: String,
    val lastReadTime: String?,
    val roomId: String
)


data class UserStatusVM(
    val userId: String,
    val isOnline: Boolean
)

data class UserStatusUpdateVM(
    val userId: String,
    val isOnline: Boolean
)


data class UserCount(
    val authenticatedUserCount: Int,
    val anonymousUserCount: Int
)
