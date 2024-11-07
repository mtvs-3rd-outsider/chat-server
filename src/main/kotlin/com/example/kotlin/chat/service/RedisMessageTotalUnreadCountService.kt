package com.example.kotlin.chat.service


import com.example.kotlin.chat.repository.Participant
import com.example.kotlin.chat.repository.ParticipantRepository
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer
import org.springframework.stereotype.Service

@Service
class RedisMessageTotalUnreadCountService(
    private val participantRepository: ParticipantRepository,
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val listenerContainer: ReactiveRedisMessageListenerContainer,
    private val objectMapper: ObjectMapper,
    private val userStatusService: UserStatusService,  // userStatusService 추가
    @Qualifier("totalUnreadMessageCount") private val messageTopic: ChannelTopic
) : RealtimeEventService<TotalUnreadMessageCountVM, MessageVM> {

    // 이중 맵 구조로 userId와 roomId에 따른 unread count 관리
    private val unreadCountStreams: MutableMap<String, MutableMap<String, MutableSharedFlow<Int>>> = mutableMapOf()

    init {
        // Redis 토픽 구독 및 메시지 수신
        GlobalScope.launch {
            listenerContainer.receive(messageTopic)
                .asFlow()
                .collect { message ->
                    val messageString = message.message as String
                    val deserializedMessage = objectMapper.readValue(messageString, TotalUnreadMessageCountVM::class.java)
                    val userId = deserializedMessage.userId
                    val roomId = deserializedMessage.roomId

                    // Redis에서 수신한 데이터를 SharedFlow에 반영
                    getOrCreateStream(userId, roomId).emit(deserializedMessage.unreadMessageCount)
                }
        }
    }

    // 특정 userId와 roomId에 대한 SharedFlow를 가져오거나 없으면 생성
    private fun getOrCreateStream(userId: String, roomId: String): MutableSharedFlow<Int> {
        val userRoomMap = unreadCountStreams.getOrPut(userId) { mutableMapOf() }
        return userRoomMap.getOrPut(roomId) { MutableSharedFlow(replay = 1) }
    }

    // 사용자의 총 읽지 않은 메시지 개수를 계산하여 반환
    override fun stream(userId: String): Flow<TotalUnreadMessageCountVM> {
//        val userRooms = unreadCountStreams[userId]?.values ?: emptyList()
//        return userRooms.combine { unreadCounts ->
//            TotalUnreadMessageCountVM(userId = userId, unreadMessageCount = unreadCounts.sum())
//        }
        return TODO("Provide the return value")
    }

    // userId에 해당하는 최신 총 읽지 않은 메시지 개수를 Redis에서 조회하여 가져옴
    override suspend fun latest(userId: String): Flow<TotalUnreadMessageCountVM> {
//        val participants = participantRepository.findByUserId(userId.toLong())
//        val totalUnreadCount = participants.map { it.unreadMessageCount }.sum()
//
//        return flow {
//            emit(TotalUnreadMessageCountVM(userId = userId, unreadMessageCount = totalUnreadCount))
//        }

        return TODO("Provide the return value")
    }

    // 메시지가 도착했을 때 Redis를 통해 읽지 않은 메시지 개수를 업데이트하는 메서드
    override suspend fun post(inboundMessages: Flow<MessageVM>) {
        inboundMessages.collect { message ->
            val roomId = message.roomId.toLong()
            val participants: Flow<Participant> = participantRepository.findByThreadId(roomId)

            participants.collect { participant ->
                val userId = participant.userId.toString()

                // userStatusService로 사용자 온라인 상태 확인
                if (!userStatusService.isUserOnline(userId, roomId.toString())) {
                    // 사용자가 오프라인 상태일 때에만 읽지 않은 메시지 개수 업데이트
                    val currentUnreadCount = getOrCreateStream(userId, roomId.toString()).replayCache.firstOrNull() ?: 0
                    val updatedUnreadCount = currentUnreadCount + 1

                    // 업데이트된 메시지 수를 Redis에 발행
                    val totalUnreadMessage = TotalUnreadMessageCountVM(userId = userId, roomId = roomId.toString(), unreadMessageCount = updatedUnreadCount)
                    val messageString = objectMapper.writeValueAsString(totalUnreadMessage)
                    redisTemplate.convertAndSend(messageTopic.topic, messageString).subscribe()
                }
            }
        }
    }
}