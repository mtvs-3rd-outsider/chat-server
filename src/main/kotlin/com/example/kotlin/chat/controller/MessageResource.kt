package com.example.kotlin.chat.controller

import NoRedisUserStatusService
import com.example.kotlin.chat.service.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactor.mono
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Controller
import reactor.core.publisher.Mono

@Controller
@MessageMapping("api.v1.messages")
class MessageResource(
    val messageThreadListInfoService: RealtimeEventService<Map<String, RoomInfoVM>, MessageVM>,
//    @Qualifier("redisMessageTotalUnreadCountService") val messageUnreadCountService: RealtimeEventService<TotalUnreadMessageCountVM, MessageVM>,
    val userLastReadTimeService: RealtimeEventService<Map<String, UserLastReadTimeVM>, MessageVM>,
    val messageService: MessageService,
    val userStatusService: UserStatusService
) {

    @MessageMapping("stream/{roomId}")
    suspend fun receive(
        @Payload inboundMessages: Flow<MessageVM>,
        @DestinationVariable roomId: String,
        @AuthenticationPrincipal principal: Jwt
    ) {
        val userId = principal.claims["userId"]?.let {
            when (it) {
                is String -> it
                is Int -> it.toString()
                else -> throw IllegalArgumentException("Unsupported userId type")
            }
        } ?: throw IllegalArgumentException("userId is missing")

        println("User ID: $userId")

        val messagesWithUserId = inboundMessages.map { message ->
            message.copy(
                roomId = roomId,
                user = message.user.copy(id = userId)
            )
        }.buffer() // 첫 메시지 보존

        coroutineScope {
            val sharedFlow = MutableSharedFlow<MessageVM>(replay = 1) // 1개의 메시지 버퍼링

            // 메시지를 buffer에 emit하여 모든 서비스가 수신할 수 있도록 함
            launch {
                messagesWithUserId.collect {
                    sharedFlow.emit(it)
                }
            }

            // 각 서비스에 공유된 메시지 스트림 전달
            launch { messageService.post(sharedFlow) }
            launch { messageThreadListInfoService.post(sharedFlow) }
//            launch { messageUnreadCountService.post(sharedFlow) }
            launch { userLastReadTimeService.post(sharedFlow) }
        }
    }
    @MessageMapping("stream/{roomId}")
    fun send(@DestinationVariable roomId: String): Flow<MessageVM> = messageService
        .stream(roomId)
//        .onStart {
//            emitAll(messageService.latest(roomId))
//        }


    @MessageMapping("threadInfos/{userId}")
    fun send2(@DestinationVariable userId: String): Flow<Map<String, RoomInfoVM>> = messageThreadListInfoService
        .stream(userId)
            .onStart {
                emitAll(messageThreadListInfoService.latest(userId))
            }
//
//    @MessageMapping("unreadCount/{userId}")
//    fun send3(@DestinationVariable userId: String): Flow<TotalUnreadMessageCountVM> = messageUnreadCountService
//        .stream(userId)
//        .onStart {
//            emitAll(messageUnreadCountService.latest(userId))
//        }


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
    fun getAllLastReadTimes(@DestinationVariable roomId: String): Flow<Map<String, UserLastReadTimeVM>> =
        userLastReadTimeService.stream(roomId).onStart {
            emitAll(userLastReadTimeService.latest(roomId))
        }


    @MessageMapping("connect/{roomId}")
    fun connect(
        @DestinationVariable roomId: String,
        @AuthenticationPrincipal principal: Jwt,
        requester: RSocketRequester
    ): Flow<Unit> = callbackFlow {
        println("Connecting for $roomId")

        val userId = principal.claims["userId"]?.let {
            when (it) {
                is String -> it
                is Int -> it.toString()
                else -> throw IllegalArgumentException("Unsupported userId type")
            }
        } ?: throw IllegalArgumentException("userId is missing")

        println("User ID: $userId")

        // 사용자를 온라인 상태로 설정
        userStatusService.setUserOnline(userId, roomId)

        // RSocket 연결이 닫힐 때 오프라인 상태 설정
        requester.rsocket()?.onClose()?.doFinally {
            println("onClose triggered for $userId in room $roomId")

            // 오프라인 상태 설정
            launch {
                userStatusService.setUserOffline(userId, roomId)
            }

            close()  // `callbackFlow` 종료를 요청하여 `awaitClose`를 트리거
        }?.subscribe()

        // 주기적으로 emit하여 연결 유지
//        while (isActive) {
//            trySend(Unit)
//            kotlinx.coroutines.delay(1000L)
//        }

        // `callbackFlow`가 종료될 때 오프라인 상태 설정 보장
        awaitClose {
            println("Disconnecting for $roomId")
//            launch {
//                userStatusService.setUserOffline(userId, roomId)
//            }
        }
    }

}
