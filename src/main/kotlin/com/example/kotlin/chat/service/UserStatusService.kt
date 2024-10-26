package com.example.kotlin.chat.service

interface UserStatusService {
    suspend fun setUserOnline(userId: String, roomId: String)
    suspend fun setUserOffline(userId: String, roomId: String)
    suspend fun isUserOnline(userId: String, roomId: String): Boolean
}