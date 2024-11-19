package com.example.kotlin.chat.controller

import com.example.kotlin.chat.service.UserStatusMonitoringService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Controller

@Controller
@MessageMapping("api.v1.status")
class UserStatusController(
    private val userStatusMonitoringService: UserStatusMonitoringService
) {

    @MessageMapping("connect")
    fun connect(
        @AuthenticationPrincipal principal: Jwt?,
        requester: RSocketRequester
    ): Flow<Unit> = callbackFlow {
        // userId 존재 여부에 따라 인증 사용자와 익명 사용자 구분
        val userId = principal?.claims?.get("userId")?.toString()

        if (userId != null) {
            println("Authenticated user $userId ")
            userStatusMonitoringService.incrementAuthenticatedUser()
        } else {
            println("Anonymous user connected")
            userStatusMonitoringService.incrementAnonymousUser()
        }

        // RSocket 연결 종료 이벤트 처리
        requester.rsocket()?.onClose()?.doFinally {
            if (userId != null) {
                println("Authenticated user $userId disconnected")
                launch { userStatusMonitoringService.decrementAuthenticatedUser() }
            } else {
                println("Anonymous user disconnected ")
                launch { userStatusMonitoringService.decrementAnonymousUser() }
            }
            close()
        }?.subscribe()

        awaitClose {
            if (userId != null) {
                println("Cleaning up resources for authenticated user $userId ")
                launch { userStatusMonitoringService.decrementAuthenticatedUser() }
            } else {
                println("Cleaning up resources for anonymous user ")
                launch { userStatusMonitoringService.decrementAnonymousUser() }
            }
        }
    }
}
