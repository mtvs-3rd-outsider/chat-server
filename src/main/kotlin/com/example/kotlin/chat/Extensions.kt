package com.example.kotlin.chat

import com.example.kotlin.chat.repository.ContentType
import com.example.kotlin.chat.repository.Message
import com.example.kotlin.chat.repository.MessageReaction
import com.example.kotlin.chat.service.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.*

fun MessageVM.asDomainObject(contentType: ContentType = ContentType.MARKDOWN): Message = Message(
    content = this.content,
    contentTypeStr = contentType.name.uppercase(Locale.getDefault()),
    sent = this.sent,
    username = this.user.name,
    userAvatarImageLink = this.user.avatarImageLink.toString(),
    roomId = this.roomId, // roomId 추가
    id = this.id,
    userId = this.user.id,
    replyToMessageId = this.replyToMessageId
)

fun Message.asViewModel(): MessageVM = MessageVM(
    content = contentType.render(this.content),
    user = UserVM(this.username, this.userAvatarImageLink,userId),
    sent = this.sent,
    roomId = this.roomId, // roomId 추가
    id = this.id,
    replyToMessageId = this.replyToMessageId,
    contentType = contentTypeStr
)

fun MessageVM.asRendered(contentType: ContentType = ContentType.MARKDOWN): MessageVM =
    this.copy(content = contentType.render(this.content))

fun Flow<Message>.mapToViewModel(): Flow<MessageVM> = map { it.asViewModel() }

fun ContentType.render(content: String): String = when (this) {
    ContentType.PLAIN -> content
    ContentType.MARKDOWN -> {
        content
    }
    ContentType.FEED -> content
    ContentType.IMAGE -> content
    ContentType.VIDEO -> content
}
//fun MessageReaction.toViewModel(): MessageReactionVM {
//    return MessageReactionVM(
//        id = this.id,
//        messageId = this.messageId,
//        userId = this.userId,
//        reactionType = this.reactionType,
//        roomId = this.roomId
//
//    )
//}

fun MessageReactionVM.toDomainObject(): MessageReaction {
    return MessageReaction(
        id = this.id,
        messageId = this.messageId,
        userId = this.userId,
        reactionType = this.reactionType,
        roomId = this.roomId,
    )
}

