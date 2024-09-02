package com.outsider.mop.chat.controller

import com.outsider.mop.chat.service.MessageService
import com.outsider.mop.chat.service.MessageVM
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Controller

@Controller
@MessageMapping("api.v1.messages")
class MessageResource(val messageService: MessageService) {

    @MessageMapping("stream/{roomId}")
    suspend fun receive(@Payload inboundMessages: Flow<MessageVM>, @DestinationVariable roomId: Long) =
        messageService.post(inboundMessages.map { it.copy(roomId = roomId) })

    @MessageMapping("stream/{roomId}")
    fun send(@DestinationVariable roomId: String): Flow<MessageVM> = messageService
        .stream()
        .onStart {
            emitAll(messageService.latest(roomId))
        }
}
