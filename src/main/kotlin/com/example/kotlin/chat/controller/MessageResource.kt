package com.example.kotlin.chat.controller

import com.example.kotlin.chat.service.MessageReactionVM
import com.example.kotlin.chat.service.MessageService
import com.example.kotlin.chat.service.MessageVM
import com.example.kotlin.chat.service.MessageWithReactionVM
import kotlinx.coroutines.flow.*
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Controller
import java.time.Instant

@Controller
@MessageMapping("api.v1.messages")
class MessageResource(val messageService: MessageService) {

    @MessageMapping("stream/{roomId}")
    suspend fun receive(@Payload inboundMessages: Flow<MessageVM>, @DestinationVariable roomId: String) =
        messageService.post(inboundMessages.map { it.copy(roomId = roomId) })

    @MessageMapping("stream/{roomId}")
    fun send(@DestinationVariable roomId: String): Flow<MessageVM> = messageService
        .stream(roomId)
        .onStart {
            emitAll(messageService.latest(roomId))
        }

    @MessageMapping("stream/{roomId}/{messageId}")
    fun send(@DestinationVariable roomId: String, @DestinationVariable messageId: Int): Flow<MessageVM> {
        return messageService
            .after(messageId, roomId)
            .onStart {
                emitAll(messageService.latest(roomId))
            }
    }
//    // 특정 채팅방에서 반응을 조회 (페이지네이션)
//    @MessageMapping("stream2/{roomId}")
//    suspend fun sendReactions(
//        @DestinationVariable roomId: String,
//        @Payload lastSent: String? // 마지막 sent 시간을 받음
//    ): Flow<MessageWithReactionVM> {
//        return messageService
//            .streamWithReaction() // 마지막 sent가 없으면 현재 시간 기준
//            .onStart {
//                emitAll(messageService.getBeforeSent(roomId, lastSent ?: Instant.now().toString())) // 초기 메시지 방출
//            }
//
//    }


}
