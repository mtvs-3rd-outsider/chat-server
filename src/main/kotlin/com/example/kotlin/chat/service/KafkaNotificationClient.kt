package com.example.kotlin.chat.service

import com.fasterxml.jackson.databind.ObjectMapper
//import org.springframework.kafka.core.ReactiveKafkaProducerTemplate
//import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate
import org.springframework.stereotype.Service
import kotlinx.coroutines.reactor.awaitSingleOrNull

//@Service
//class KafkaNotificationClient(
//    private val kafkaTemplate: ReactiveKafkaProducerTemplate<String, String>,
//    private val objectMapper: ObjectMapper
//) : NotificationClient {
//
//    override suspend fun sendNotification(updateMessageDTO: UpdateMessageDTO) {
//        try {
//            // UpdateMessageDTO를 JSON 문자열로 변환
//            val notificationMessage = objectMapper.writeValueAsString(updateMessageDTO)
//
//            // ReactiveKafkaProducerTemplate을 사용하여 비동기로 메시지 전송
//            kafkaTemplate.send("update-messages", notificationMessage)
//                .awaitSingleOrNull()  // 코루틴 방식으로 결과 기다림
//
//            println("Notification sent to Kafka: $notificationMessage")
//        } catch (e: Exception) {
//            println("Failed to send notification: ${e.message}")
//            throw e  // 필요에 따라 예외 처리
//        }
//    }
//}
