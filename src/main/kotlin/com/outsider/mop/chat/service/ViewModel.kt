package com.outsider.mop.chat.service

import java.net.URI
import java.time.Instant

data class MessageVM(
    val content: String,
    val user: UserVM,
    val sent: Instant,
    val roomId: Long,  // roomId 추가
    val id: Long? = null
)

data class UserVM(
    val name: String,
    val avatarImageLink: URI
)
