package com.example.kotlin.chat.controller

import com.example.kotlin.chat.service.MessageReactionService
import com.example.kotlin.chat.service.MessageReactionVM
import com.example.kotlin.chat.service.MessageVM
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Controller
import java.time.Instant

@Controller
@MessageMapping("api.v1.reactions")
class MessageReactionResource(val messageReactionService: MessageReactionService) {


    // 반응을 저장
    @MessageMapping("stream/{roomId}")
    suspend fun receive(@Payload inboundReactions: Flow<MessageReactionVM>, @DestinationVariable roomId: String) =
        messageReactionService.post(inboundReactions.map { it.copy(roomId = roomId) })

    @MessageMapping("stream/{roomId}")
    fun send(@DestinationVariable roomId: String): Flow<MessageReactionVM> = messageReactionService
        .stream(roomId)

}
