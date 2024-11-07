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
    contentTypeStr = this.contentType.uppercase(Locale.getDefault()),
    sent = this.sent,
    roomId = this.roomId, // roomId 추가
    id = this.id,
    userId = this.user.id,
    replyToMessageId = this.replyToMessageId,
    replyContent = this.replyContent,
    mediaUrl = this.mediaUrl
)





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

