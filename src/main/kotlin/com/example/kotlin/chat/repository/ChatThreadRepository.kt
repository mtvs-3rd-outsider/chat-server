package com.example.kotlin.chat.repository

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface ChatThreadRepository : CoroutineCrudRepository<ChatThread, Long> {
}
