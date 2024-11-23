package com.example.kotlin.chat.controller

import com.example.kotlin.chat.service.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
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
import reactor.core.scheduler.Schedulers

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
                is Long -> it.toString()
                else -> throw IllegalArgumentException("Unsupported userId type")
            }
        } ?: throw IllegalArgumentException("userId is missing")




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
                launch { userLastReadTimeService.post(sharedFlow) }

        }
    }
    @MessageMapping("stream.betting/{roomId}")
    suspend fun receive2(
        @Payload inboundMessages: Flow<MessageVM>,
        @DestinationVariable roomId: String,
        @AuthenticationPrincipal principal: Jwt
    ) {
        val userId = principal.claims["userId"]?.let {
            when (it) {
                is String -> it
                is Int -> it.toString()
                is Long -> it.toString()
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
                println("Invalid roomId format: Using only messageService")
                launch { messageService.post(sharedFlow) }
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
                is Long -> it.toString()
                else -> throw IllegalArgumentException("Unsupported userId type")
            }
        } ?: throw IllegalArgumentException("userId is missing")

        println("User ID: $userId")

        // 사용자를 온라인 상태로 설정
        try {
            userStatusService.setUserOnline(userId, roomId)
        } catch (e: Exception) {
            println("Error setting user online: ${e.message}")
            close(e) // Flow 종료
            return@callbackFlow
        }

        // RSocket 연결 종료 처리
        requester.rsocket()?.onClose()?.doFinally {
            println("onClose triggered for $userId in room $roomId")

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    userStatusService.setUserOffline(userId, roomId)
                } catch (e: Exception) {
                    println("Error setting user offline: ${e.message}")
                } finally {
                    close()
                }
            }
        }?.subscribe()


        // 주기적으로 emit하여 연결 유지
        while (isActive) {
            trySend(Unit)
            kotlinx.coroutines.delay(1000L)
        }

        // Flow 종료 시 오프라인 상태 설정
        awaitClose {
            println("Disconnecting for $roomId")

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    userStatusService.setUserOffline(userId, roomId) // 병렬로 실행
                } catch (e: Exception) {
                    println("Error during disconnect: ${e.message}")
                }
            }
        }
    }




}
