package com.example.kotlin.chat.service

import com.example.kotlin.chat.asDomainObject
import com.example.kotlin.chat.asRendered
import com.example.kotlin.chat.mapToViewModel
import com.example.kotlin.chat.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.collect
import org.springframework.stereotype.Service

@Service
class PersistentMessageService(val messageRepository: MessageRepository) : MessageService {

    val sender: MutableSharedFlow<MessageVM> = MutableSharedFlow()

    // 특정 채팅방의 최신 메시지 가져오기
    override fun latest(roomId: String): Flow<MessageVM> =
        messageRepository.findLatestAfterMessageInRoom(roomId)
            .mapToViewModel()

    // 특정 메시지 이후의 메시지 가져오기
    override fun after(messageId: Int, roomId: String): Flow<MessageVM> =
        messageRepository.findLatestAfterMessageInRoom(messageId, roomId)
            .mapToViewModel()

    // 실시간 메시지 스트림
    override fun stream(): Flow<MessageVM> = sender

    // 메시지 전송
    override suspend fun post(messages: Flow<MessageVM>) =
        messages
            .onEach { sender.emit(it.asRendered()) }
            .map { it.asDomainObject() }
            .let { messageRepository.saveAll(it) }
            .collect()
}
