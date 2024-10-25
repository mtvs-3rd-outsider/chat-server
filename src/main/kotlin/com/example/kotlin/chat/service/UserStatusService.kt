package com.example.kotlin.chat.service

import org.springframework.stereotype.Service

@Service
class UserStatusService {

    private val userOnlineStatus: MutableMap<String, Boolean> = mutableMapOf()

    // 사용자가 온라인일 때 true로 설정
    fun setUserOnline(userId: String) {
        userOnlineStatus[userId] = true
        println("User $userId is now online.")
    }

    // 사용자가 오프라인일 때 false로 설정
    fun setUserOffline(userId: String) {
        userOnlineStatus[userId] = false
        println("User $userId is now offline.")
    }

    // 사용자 상태를 조회
    fun isUserOnline(userId: String): Boolean {
        return userOnlineStatus.getOrDefault(userId, false)
    }
}
