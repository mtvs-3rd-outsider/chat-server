package com.example.kotlin.chat.service


import com.example.kotlin.chat.mapToViewModel
import com.example.kotlin.chat.repository.MessageReactionRepository
import com.example.kotlin.chat.toDomainObject
import kotlinx.coroutines.flow.*
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class MessageReactionServiceImpl(val messageReactionRepository: MessageReactionRepository): MessageReactionService {

    // 각 roomId에 대응하는 스트림을 관리하는 맵
    private val roomStreams: MutableMap<String, MutableSharedFlow<MessageReactionVM>> = mutableMapOf()


    // 반응을 저장하고 실시간으로 각 roomId에 맞는 스트림에 전송
    override suspend fun post(reactions: Flow<MessageReactionVM>) {
        reactions
            .onEach { reaction ->
                val roomId = reaction.roomId // 각 반응의 roomId를 사용하여 스트림을 선택
                getOrCreateStream(roomId).emit(reaction) // 해당 roomId의 스트림에 반응 전송
            }
            .collect { reaction ->
                if (reaction.isPlus) {
                    // 반응을 추가하는 경우 (save)
                    messageReactionRepository.save(reaction.toDomainObject())
                } else {
                    // 반응을 제거하는 경우 (delete)
                    messageReactionRepository.delete(reaction.toDomainObject())
                }
            }
    }
    private fun getOrCreateStream(roomId: String): MutableSharedFlow<MessageReactionVM> {
        return roomStreams.getOrPut(roomId) {
            MutableSharedFlow() // 새 스트림 생성 (replay 설정은 필요에 따라 조정)
        }
    }

    override fun stream(roomId: String): Flow<MessageReactionVM> {
        return getOrCreateStream(roomId)
    }
//    override fun latest(roomId: String): Flow<MessageVM> =
//        messageReactionRepository.findLatestAfterMessageInRoom(roomId)
//            .mapToViewModel()
//    // 특정 메시지 이후의 메시지 가져오기
//    override fun after(messageId: Int, roomId: String): Flow<MessageVM> =
//        messageReactionRepository.findLatestAfterMessageInRoom(messageId, roomId)
//            .mapToViewModel()

}

