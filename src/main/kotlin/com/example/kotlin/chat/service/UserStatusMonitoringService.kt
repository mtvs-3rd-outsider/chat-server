package com.example.kotlin.chat.service
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service

@Service
class UserStatusMonitoringService(private val meterRegistry: MeterRegistry) {

    private var authenticatedUserCount = 0.0
    private var anonymousUserCount = 0.0

    init {
        // 인증된 사용자와 익명 사용자의 메트릭 설정
        meterRegistry.gauge("authenticated_users", this) { it.authenticatedUserCount }
        meterRegistry.gauge("anonymous_users", this) { it.anonymousUserCount }
    }

    private fun getCurrentUserCount(): UserCount {
        val authenticatedCount = getAuthenticatedUserCount()
        val anonymousCount = getAnonymousUserCount()
        return UserCount(authenticatedCount, anonymousCount)
    }

    // 실시간 사용자 수 상태를 위한 MutableSharedFlow 생성
    private val userCountFlow = MutableSharedFlow<UserCount>(replay = 1)

    /**
     * 최신 사용자 상태를 반환하는 함수
     */
    fun latest(): Flow<UserCount> = flow {
        emit(getCurrentUserCount()) // 최신 사용자 상태를 한 번 방출
    }

    /**
     * 실시간 사용자 상태 스트림을 반환하는 함수
     */
    fun stream(): Flow<UserCount> = userCountFlow.asSharedFlow()

    // 인증 사용자 증가
    fun incrementAuthenticatedUser() {
        authenticatedUserCount += 1
        emitUserCount()
    }

    // 인증 사용자 감소
    fun decrementAuthenticatedUser() {
        authenticatedUserCount -= 1
        emitUserCount()
    }

    // 익명 사용자 증가
    fun incrementAnonymousUser() {
        anonymousUserCount += 1
        emitUserCount()
    }

    // 익명 사용자 감소
    fun decrementAnonymousUser() {
        anonymousUserCount -= 1
        emitUserCount()
    }

    private fun emitUserCount() {
        runBlocking {
            userCountFlow.emit(getCurrentUserCount())
        }
    }

    fun getAuthenticatedUserCount(): Int = authenticatedUserCount.toInt()
    fun getAnonymousUserCount(): Int = anonymousUserCount.toInt()
}

