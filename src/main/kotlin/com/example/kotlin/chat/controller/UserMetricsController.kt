package com.example.kotlin.chat.controller

import com.example.kotlin.chat.service.UserCount
import com.example.kotlin.chat.service.UserStatusMonitoringService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.annotation.ConnectMapping
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Controller
import reactor.core.publisher.Mono
import java.time.Instant
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Controller
@MessageMapping("api.v1.status")
class UserStatusController(
    private val userStatusMonitoringService: UserStatusMonitoringService
) {

    private val logger = LoggerFactory.getLogger(UserStatusController::class.java)

    /**
     * 실시간 사용자 상태 스트림을 반환하는 엔드포인트
     */

    @MessageMapping("user-counts")
    fun streamUserCounts(): Flow<UserCount> = userStatusMonitoringService.stream()
        .onStart {
            emitAll(userStatusMonitoringService.latest())
        }

    // 클라이언트별로 연결 정보를 저장하기 위한 맵
    private val sessionInfoMap = ConcurrentHashMap<String, SessionInfo>()
    @MessageMapping("connect")
    fun connect(
        @AuthenticationPrincipal principal: Jwt?,
        requester: RSocketRequester,
        @Payload request: PageViewRequest
    ): Mono<Void> {
        println("connect!!!!!!!!!")
        println(request)
        return Mono.create<Void>  { sink ->
            val connectTimestamp = Instant.now()
            val userId = getUserId(principal)
            val sessionId = requester.rsocket()?.hashCode()?.toString() ?: "unknown"
            val sessionInfo = SessionInfo(userId, request.pagePath, connectTimestamp)
            sessionInfoMap[sessionId] = sessionInfo

            handleUserConnection(userId)

            // RSocket 연결 종료 이벤트 처리
            requester.rsocket()?.onClose()?.doFinally {
                val disconnectTimestamp = Instant.now()
                val dwellTime = calculateDwellTime(sessionInfo.connectTime, disconnectTimestamp)
                logConnection(sessionInfo, disconnectTimestamp)
                handleUserDisconnection(userId)
                sessionInfoMap.remove(sessionId)
                println("disconnect!!!!!!!!!")

            }?.subscribe()

            // Mono 종료 시 호출되는 작업 정의
//            sink.onDispose {
//                val disconnectTimestamp = Instant.now()
//                val dwellTime = calculateDwellTime(sessionInfo.connectTime, disconnectTimestamp)
//                logConnection(sessionInfo, disconnectTimestamp)
//                handleUserDisconnection(userId)
//                sessionInfoMap.remove(sessionId)
//                println("disconnect2!!!!!!!!!")
//            }
        }.then() // Mono<Void> 반환
    }
    /**
     * 클라이언트의 연결 이벤트를 처리하는 엔드포인트
     */
//    @MessageMapping("connect")
//    fun onConnect(
//        @AuthenticationPrincipal principal: Jwt?,
//        requester: RSocketRequester,
//        @Payload request: PageViewRequest
//    ) : Mono<Void> {
//        println("connect!!!!!!!!!!!!!!")
//        val connectTimestamp = Instant.now()
//        val userId = getUserId(principal)
//        val sessionId = requester.rsocket()?.hashCode()?.toString() ?: "unknown"
//
//        // 연결 정보 저장
//        val sessionInfo = SessionInfo(userId, request.pagePath, connectTimestamp)
//        sessionInfoMap[sessionId] = sessionInfo
//
//        handleUserConnection(userId)
//
//        // 클라이언트의 연결 해제 이벤트 처리
//        requester.rsocket()?.onClose()?.subscribe {
//            val disconnectTimestamp = Instant.now()
//            val startTime = sessionInfo.connectTime
//            val dwellTime = calculateDwellTime(startTime, disconnectTimestamp)
//            logConnection(sessionInfo, disconnectTimestamp)
//            handleUserDisconnection(userId)
//            // 연결 정보 제거
//            sessionInfoMap.remove(sessionId)
//        }
//    }


    /**
     * 개별 게시글(피드)에 대한 뷰 체류 시간을 로그로 수집하는 엔드포인트
     */
    @MessageMapping("log-view")
    fun logView(
        @AuthenticationPrincipal principal: Jwt?,
       @Payload request: FeedViewRequest
    ): Mono<Void> {
        return Mono.create<Void> { sink ->
            try {
                val userId = getUserId(principal)
                val timestamp = Instant.now()
                logFeedView(userId, request, timestamp)
                sink.success() // 작업이 성공적으로 완료되었음을 알림
            } catch (ex: Exception) {
                sink.error(ex) // 예외 발생 시 Mono에 에러를 전달
            }
        }
    }



    // 사용자 ID를 추출하는 함수
    private fun getUserId(principal: Jwt?): String? {
        return principal?.claims?.get("userId")?.toString()
    }

    // 사용자 연결 처리를 담당하는 함수
    private fun handleUserConnection(userId: String?) {
        if (userId != null) {
            logger.info("Authenticated user $userId connected")
            userStatusMonitoringService.incrementAuthenticatedUser()
        } else {
            logger.info("Anonymous user connected")
            userStatusMonitoringService.incrementAnonymousUser()
        }
    }

    // 사용자 연결 해제를 처리하는 함수
    private fun handleUserDisconnection(userId: String?) {
        if (userId != null) {
            logger.info("Authenticated user $userId disconnected")
            userStatusMonitoringService.decrementAuthenticatedUser()
        } else {
            logger.info("Anonymous user disconnected")
            userStatusMonitoringService.decrementAnonymousUser()
        }
    }


    // 체류 시간을 계산하는 함수
    private fun calculateDwellTime(connectTime: Instant, disconnectTime: Instant): Long {
        return Duration.between(connectTime, disconnectTime).seconds
    }

    // 페이지별 체류 시간을 로그로 기록하는 함수
    private fun logPageView(
        userId: String?,
        request: PageViewRequest,
        dwellTime: Long,
        timestamp: Instant
    ) {
        val actualUserId = userId ?: "anonymous"
        logger.info(
            "Page View - userId: $actualUserId, timestamp: $timestamp, " +
                    "pagePath: ${request.pagePath}, dwellTime: $dwellTime seconds"
        )
    }
    // 연결 정보를 로그로 기록하는 함수
    private fun logConnection(
        sessionInfo: SessionInfo,
        disconnectTimestamp: Instant
    ) {
        val userId = sessionInfo.userId ?: "anonymous"
        val pagePath = sessionInfo.pagePath
        val connectTime = sessionInfo.connectTime
        val dwellTime = calculateDwellTime(connectTime, disconnectTimestamp)
        logger.info(
            "Connection - userId: $userId, pagePath: $pagePath, " +
                    "startTime: $connectTime, endTime: $disconnectTimestamp, " +
                    "dwellTime: $dwellTime seconds"
        )
    }
    // 세션 정보를 저장하는 데이터 클래스
    data class SessionInfo(
        val userId: String?,
        val pagePath: String,
        val connectTime: Instant
    )
    // 개별 피드 뷰를 로그로 기록하는 함수
    private fun logFeedView(
        userId: String?,
        request: FeedViewRequest,
        timestamp: Instant
    ) {
        val actualUserId = userId ?: "anonymous"
        logger.info(
            "Feed View - userId: $actualUserId, timestamp: $timestamp, " +
                    "feedId: ${request.feedId}, actionType: ${request.actionType}, " +
                    "start: ${request.start}, end: ${request.end}"
        )
    }
}

// 페이지 뷰 요청 데이터를 캡슐화하는 데이터 클래스
data class PageViewRequest(
    val pagePath: String
)

// 피드 뷰 요청 데이터를 캡슐화하는 데이터 클래스
data class FeedViewRequest(
    val feedId: String,
    val actionType: String = "view",
    val start: Long,
    val end: Long
)
