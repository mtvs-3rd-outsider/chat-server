package com.example.kotlin.chat.service

import com.example.kotlin.chat.repository.ChatThreadRepository
import com.example.kotlin.chat.repository.ParticipantRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

import kotlinx.coroutines.flow.toList
@Service
class NotificationService(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    private val chatThreadRepository: ChatThreadRepository,
    private val participantRepository: ParticipantRepository,
    private val userStatusService: UserStatusService
) {

    suspend fun sendNotification(messageVM: MessageVM) {
        val roomId = messageVM.roomId
        val chatThread = chatThreadRepository.findById(roomId.toLong())
        if (chatThread == null) {
            println("No chat thread found for roomId: $roomId")
            return
        }

        val participants = participantRepository.findByThreadId(roomId.toLong()).toList()
        val recipients = mutableListOf<String>()

        for (participant in participants) {
            val userId = participant.userId.toString()

            // 오프라인 사용자만 대상으로 포함
            if (!userStatusService.isUserOnline(userId, roomId)) {
                recipients.add(userId)
            }
        }

        // 수신자가 없으면 중단
        if (recipients.isEmpty()) {
            println("No offline users to notify for roomId: $roomId")
            return
        }

        // 알림 메시지 생성
        val notification = mapOf(
            "senderUserId" to messageVM.user.id,
            "content" to messageVM.content,
            "roomId" to messageVM.roomId,
            "sent" to messageVM.sent.toString(),
            "recipients" to recipients // userId 리스트만 포함
        )

        // Kafka로 메시지 전송
        try {
            val messageString = objectMapper.writeValueAsString(notification)
            kafkaTemplate.send("user-notifications", messageString)
            println("Kafka notification sent: $messageString")
        } catch (e: Exception) {
            println("Failed to send Kafka notification: ${e.message}")
        }
    }
}
