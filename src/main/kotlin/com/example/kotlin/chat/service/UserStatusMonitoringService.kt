package com.example.kotlin.chat.service
import io.micrometer.core.instrument.MeterRegistry
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

    // 인증 사용자 증가
    fun incrementAuthenticatedUser() {
        authenticatedUserCount += 1
    }

    // 인증 사용자 감소
    fun decrementAuthenticatedUser() {
        authenticatedUserCount -= 1
    }

    // 익명 사용자 증가
    fun incrementAnonymousUser() {
        anonymousUserCount += 1
    }

    // 익명 사용자 감소
    fun decrementAnonymousUser() {
        anonymousUserCount -= 1
    }

    fun getAuthenticatedUserCount(): Int = authenticatedUserCount.toInt()
    fun getAnonymousUserCount(): Int = anonymousUserCount.toInt()
}
