package com.example.kotlin.chat.controller

import com.example.kotlin.chat.service.*
import kotlinx.coroutines.flow.*
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.stereotype.Controller
import reactor.core.publisher.Mono

@Controller
@MessageMapping("api.v1.messages")
class MessageResource(
    val messageThreadListInfoService: RealtimeEventService<List<RoomInfoVM>, MessageVM>,
    val messageUnreadCountService: RealtimeEventService<TotalUnreadMessageCountVM, MessageVM>,
    val userLastReadTimeService: RealtimeEventService<List<UserLastReadTimeVM>, MessageVM>,
    val messageService: MessageService,
    val userStatusService: UserStatusService
) {

    @MessageMapping("stream/{roomId}")
    suspend fun receive(@Payload inboundMessages: Flow<MessageVM>, @DestinationVariable roomId: String){
        messageService.post(inboundMessages.map { it.copy(roomId = roomId) }) // 메시지 저장 로직
//        messageThreadListInfoService.post(inboundMessages.map { it.copy(roomId = roomId) }) // 메시지 저장 후 참여자 업데이트 및 알림 전송
//        messageUnreadCountService.post(inboundMessages.map { it.copy(roomId = roomId) })
//        userLastReadTimeService.post(inboundMessages.map { it.copy(roomId = roomId) }) // 새로운 읽은 시간 업데이트 로직 추가

    }

    @MessageMapping("stream/{roomId}")
    fun send(@DestinationVariable roomId: String): Flow<MessageVM> = messageService
        .stream(roomId)
        .onStart {
            emitAll(messageService.latest(roomId))
        }
    @MessageMapping("threadInfos/{userId}")
    fun send2(@DestinationVariable userId: String): Flow<List<RoomInfoVM>> = messageThreadListInfoService
        .stream(userId)
            .onStart {
                emitAll(messageThreadListInfoService.latest(userId))
            }

    @MessageMapping("unreadCount/{userId}")
    fun send3(@DestinationVariable userId: String): Flow<TotalUnreadMessageCountVM> = messageUnreadCountService
        .stream(userId)
        .onStart {
            emitAll(messageUnreadCountService.latest(userId))
        }


    @MessageMapping("stream/{roomId}/{messageId}")
    fun send(@DestinationVariable roomId: String, @DestinationVariable messageId: Int): Flow<MessageVM> {
        return messageService
            .after(messageId, roomId)
            .onStart {
                emitAll(messageService.latest(roomId))
            }
    }
    // 특정 방(roomId)에 대한 모든 사용자의 마지막 읽은 시간 정보를 제공하는 스트림
    @MessageMapping("lastReadTimes/{roomId}")
    fun getAllLastReadTimes(@DestinationVariable roomId: String): Flow<List<UserLastReadTimeVM>> =
        userLastReadTimeService.stream(roomId)

    @MessageMapping("connect/{userId}")
    fun connect(@DestinationVariable userId: String, requester: RSocketRequester): Mono<Void> {
        userStatusService.setUserOnline(userId)

        requester.rsocket()?.onClose()?.doFinally {
            userStatusService.setUserOffline(userId)
        }?.subscribe()

        return Mono.empty()
    }

}
