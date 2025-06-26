package com.example.kotlin.chat.controller

import com.example.kotlin.chat.service.ChatThreadManagementService
import com.example.kotlin.chat.service.ChatThreadDTO
import com.example.kotlin.chat.service.CreateChatThreadDTO
import com.example.kotlin.chat.service.CreateChatThreadResponse
import kotlinx.coroutines.flow.Flow
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/chat-threads")
class ChatThreadResource(private val chatThreadManagementService: ChatThreadManagementService) {

    @PostMapping("/create")
    suspend fun createChatThread(
            @RequestBody createChatThreadDTO: CreateChatThreadDTO,
            @AuthenticationPrincipal principal: Jwt
    ): ResponseEntity<CreateChatThreadResponse> {
        val userId = principal.subject
            ?: throw IllegalArgumentException("User ID (sub) not found in JWT")

        val response = chatThreadManagementService.createOrGetThread(createChatThreadDTO, userId)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/user-threads")
    suspend fun getThreadsByUserId(
            @AuthenticationPrincipal principal: Jwt
    ): ResponseEntity<Flow<ChatThreadDTO>> {
        val userId = principal.subject
            ?: throw IllegalArgumentException("User ID (sub) not found in JWT")

        val threads = chatThreadManagementService.getThreadsByUserId(userId)
        return ResponseEntity.ok(threads)
    }

    @GetMapping("/{chatRoomId}")
    suspend fun getThread(@PathVariable chatRoomId: Long): ResponseEntity<ChatThreadDTO> {
        val threadDetails = chatThreadManagementService.getThreadDetails(chatRoomId)
        return if (threadDetails != null) {
            ResponseEntity.ok(threadDetails)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/{chatRoomId}")
    suspend fun deleteChatThread(
            @PathVariable chatRoomId: Long,
            @AuthenticationPrincipal principal: Jwt
    ): ResponseEntity<Void> {
        val userId = principal.subject
            ?: throw IllegalArgumentException("User ID (sub) not found in JWT")

        chatThreadManagementService.deactivateParticipant(chatRoomId, userId)
        return ResponseEntity.noContent().build()
    }
}
