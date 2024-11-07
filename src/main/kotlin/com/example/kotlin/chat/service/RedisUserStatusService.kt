import com.example.kotlin.chat.repository.ParticipantRepository
import com.example.kotlin.chat.service.RoomInfoVM
import com.example.kotlin.chat.service.TotalUnreadMessageCountVM
import com.example.kotlin.chat.service.UserLastReadTimeVM
import com.example.kotlin.chat.service.UserStatusService
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.listener.ChannelTopic
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Service
class RedisUserStatusService(
    private val participantRepository: ParticipantRepository, // ParticipantRepository 주입
    private val redisTemplate: ReactiveRedisTemplate<String, Boolean>, // RedisTemplate 주입  ,
    private val stringRedisTemplate: ReactiveRedisTemplate<String, String>, // RedisTemplate 주입  ,
    @Qualifier("threadListInfo") private val threadListInfoTopic: ChannelTopic
,@Qualifier("userLastReadTime") private val userLastReadTimeTopic: ChannelTopic
,  private val objectMapper: ObjectMapper // ObjectMapper 주입
) : UserStatusService {

    // Redis에서 사용할 키 생성 함수
    private fun generateRedisKey(userId: String, roomId: String): String = "user:$userId:room:$roomId:online"


    override suspend fun setUserOnline(userId: String, roomId: String) {
        val redisKey = generateRedisKey(userId, roomId)
        redisTemplate.opsForValue().set(redisKey, true).awaitFirstOrNull()
        println("User $userId is now online in room $roomId.")

        val participant = participantRepository.findByUserIdAndThreadId(userId.toLong(), roomId.toLong())
        participant?.let {
            it.isOnline = true
            it.unreadMessageCount = 0
            it.lastReadTime = LocalDateTime.now()
            participantRepository.save(it)

            val lastReadTimeString = it.lastReadTime
                ?.atZone(ZoneId.systemDefault())
                ?.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

            if (lastReadTimeString != null) {
                val statusMessage = UserLastReadTimeVM(
                    userId = userId,
                    roomId = roomId,
                    lastReadTime = lastReadTimeString
                )
                val messageJson = objectMapper.writeValueAsString(statusMessage)
                stringRedisTemplate.convertAndSend(userLastReadTimeTopic.topic, messageJson).awaitFirstOrNull()
            }

            val initialRoomInfo = RoomInfoVM(
                userId = userId,
                roomId = roomId,
                roomName = "Room $roomId",
                lastMessage = "No recent messages",
                lastMessageTime = LocalDateTime.now(),
                unreadMessageCount = 0
            )

            val threadInfoJson = objectMapper.writeValueAsString(initialRoomInfo)
            stringRedisTemplate.convertAndSend(threadListInfoTopic.topic, threadInfoJson).awaitFirstOrNull()
        }
    }
    override suspend fun setUserOffline(userId: String, roomId: String) {
        val redisKey = generateRedisKey(userId, roomId)
        redisTemplate.opsForValue().set(redisKey, false).awaitFirstOrNull()
        println("User $userId is now offline in room $roomId.")

    }
    // 특정 사용자와 채팅방(roomId)의 온라인 상태를 Redis에서 조회
    override suspend fun isUserOnline(userId: String, roomId: String): Boolean {
        val redisKey = generateRedisKey(userId, roomId)
        return redisTemplate.opsForValue().get(redisKey).awaitFirstOrNull() ?: false
    }
}
