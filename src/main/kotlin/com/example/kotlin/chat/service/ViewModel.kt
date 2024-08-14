package com.example.kotlin.chat.service

import java.net.URL
import java.time.Instant

data class MessageVM(
    val content: String,
    val user: UserVM,
    val sent: Instant,
    val roomId: String,  // roomId 추가
    val id: Int? = null
)

data class UserVM(
    val name: String,
    val avatarImageLink: URL
)
