package com.example.kotlin.chat

import com.example.kotlin.chat.repository.ContentType
import com.example.kotlin.chat.repository.Message
import com.example.kotlin.chat.service.MessageVM
import com.example.kotlin.chat.service.UserVM
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import java.net.URL
import java.util.*

fun MessageVM.asDomainObject(contentType: ContentType = ContentType.MARKDOWN): Message = Message(
    content = this.content,
    contentTypeStr = contentType.name.uppercase(Locale.getDefault()),
    sent = this.sent,
    username = this.user.name,
    userAvatarImageLink = this.user.avatarImageLink.toString(),
    roomId = this.roomId, // roomId 추가
    id = this.id
)

fun Message.asViewModel(): MessageVM = MessageVM(
    content = contentType.render(this.content),
    user = UserVM(this.username, this.userAvatarImageLink),
    sent = this.sent,
    roomId = this.roomId, // roomId 추가
    id = this.id
)

fun MessageVM.asRendered(contentType: ContentType = ContentType.MARKDOWN): MessageVM =
    this.copy(content = contentType.render(this.content))

fun Flow<Message>.mapToViewModel(): Flow<MessageVM> = map { it.asViewModel() }

fun ContentType.render(content: String): String = when (this) {
    ContentType.PLAIN -> content
    ContentType.MARKDOWN -> {
        content
    }
}
