package com.example.kotlin.chat.service

import kotlinx.coroutines.flow.Flow

interface MessageReactionService {

    // 메시지를 게시 (저장하고 실시간 스트림에 전송)
    suspend fun post(reactions: Flow<MessageReactionVM>)
    fun stream(roomId: String): Flow<MessageReactionVM>
//    fun latest(roomId: String): Flow<MessageVM>
//    fun after(messageId: Int, roomId: String): Flow<MessageVM>
}