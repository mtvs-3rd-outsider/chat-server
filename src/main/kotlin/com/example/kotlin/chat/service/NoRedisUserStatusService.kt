import com.example.kotlin.chat.repository.ParticipantRepository
import com.example.kotlin.chat.service.UserStatusService
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneId

@Profile("no_redis")
@Service
class NoRedisUserStatusService(
    private val participantRepository: ParticipantRepository // ParticipantRepository 주입
): UserStatusService {

    // (userId, roomId)를 키로 사용하여 온라인 상태를 관리
    private val userOnlineStatus: MutableMap<Pair<String, String>, Boolean> = mutableMapOf()

    // 사용자가 온라인일 때 isOnline을 true로 설정하고 lastReadTime을 업데이트
    override suspend fun setUserOnline(userId: String, roomId: String) {
        userOnlineStatus[Pair(userId, roomId)] = true
        println("User $userId is now online in room $roomId.")

        // Participant 엔티티의 isOnline 및 lastReadTime 업데이트
        val participant = participantRepository.findByUserIdAndThreadId(userId.toLong(), roomId.toLong())
        participant?.let {
            it.isOnline = true
            it.lastReadTime = LocalDateTime.now(ZoneId.systemDefault()) // 현재 시간을 lastReadTime으로 설정
            participantRepository.save(it) // 변경 사항 영속화
        }
    }

    // 사용자가 오프라인일 때 isOnline을 false로 설정하고 unreadMessageCount를 증가
    override suspend fun setUserOffline(userId: String, roomId: String) {
        userOnlineStatus[Pair(userId, roomId)] = false
        println("User $userId is now offline in room $roomId.")

        // Participant 엔티티의 isOnline 및 unreadMessageCount 업데이트
        val participant = participantRepository.findByUserIdAndThreadId(userId.toLong(), roomId.toLong())
        participant?.let {
            it.isOnline = false
            it.unreadMessageCount += 1 // 오프라인일 때 unreadMessageCount 증가
            participantRepository.save(it) // 변경 사항 영속화
        }
    }

    // 특정 사용자와 채팅방(roomId)의 온라인 상태를 조회
    override suspend fun isUserOnline(userId: String, roomId: String): Boolean {
        return userOnlineStatus.getOrDefault(Pair(userId, roomId), false)
    }
}
