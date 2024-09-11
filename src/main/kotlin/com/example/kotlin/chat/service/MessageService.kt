package com.example.kotlin.chat.service

import kotlinx.coroutines.flow.Flow

interface MessageService {

    // 특정 채팅방의 최신 메시지를 가져오기 위해 roomId를 추가
    fun latest(roomId: Int): Flow<MessageVM>

    // 특정 채팅방에서 특정 메시지 이후의 메시지를 가져오기 위해 roomId를 추가
    fun after(messageId: Int, roomId: Int): Flow<MessageVM>

    // 실시간 메시지 스트림 (모든 채팅방 또는 특정 로직에 의해 필터링된 스트림)
    fun stream(): Flow<MessageVM>

    // 메시지를 게시 (저장하고 실시간 스트림에 전송)
    suspend fun post(messages: Flow<MessageVM>)
}
