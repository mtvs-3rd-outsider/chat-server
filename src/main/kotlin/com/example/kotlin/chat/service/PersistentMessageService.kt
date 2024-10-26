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
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service

@Service
@Profile("no_redis")
class PersistentMessageService(
    val messageRepository: MessageRepository
) : MessageService {

    private val roomStreams: MutableMap<String, MutableSharedFlow<MessageVM>> = mutableMapOf()
    // 특정 채팅방의 최신 메시지 가져오기
    override fun latest(roomId: String): Flow<MessageVM> =
        messageRepository.findLatestAfterMessageInRoom(roomId)
            .mapToViewModel()

    // 특정 메시지 이후의 메시지 가져오기
    override fun after(messageId: Int, roomId: String): Flow<MessageVM> =
        messageRepository.findLatestAfterMessageInRoom(messageId, roomId)
            .mapToViewModel()

    // 실시간 메시지 스트림
    override fun stream(roomId: String): Flow<MessageVM> {
        return getOrCreateStream(roomId)
    }
    private fun getOrCreateStream(roomId: String): MutableSharedFlow<MessageVM> {
        return roomStreams.getOrPut(roomId) { MutableSharedFlow() }
    }
    // 메시지 전송
    // 메시지 전송 (roomId별로 메시지를 전송하고 해당 스트림에 emit)
    override suspend fun post(messages: Flow<MessageVM>) {
        messages
            .onEach { message ->
                val roomId = message.roomId // 각 메시지의 roomId를 사용하여 스트림을 선택
                getOrCreateStream(roomId).emit(message.asRendered()) // 해당 roomId의 스트림에 메시지 전송
            }
            .map { it.asDomainObject() }
            .let { messageRepository.saveAll(it) }
            .collect()
    }


}
