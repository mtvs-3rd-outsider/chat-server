package com.example.kotlin.chat.controller

import com.example.kotlin.chat.service.ChatThreadManagementService
import com.example.kotlin.chat.service.ChatThreadDTO
import com.example.kotlin.chat.service.CreateChatThreadDTO
import com.example.kotlin.chat.service.CreateChatThreadResponse
import kotlinx.coroutines.flow.Flow
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal
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
    
    companion object {
        private val logger = LoggerFactory.getLogger(ChatThreadResource::class.java)
    }

    @PostMapping("/create")
    suspend fun createChatThread(
            @RequestBody createChatThreadDTO: CreateChatThreadDTO,
            authentication: Authentication
    ): ResponseEntity<CreateChatThreadResponse> {
        val userId = extractUserId(authentication)
            ?: throw IllegalArgumentException("User ID not found in authentication")

        val response = chatThreadManagementService.createOrGetThread(createChatThreadDTO, userId)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/user-threads")
    suspend fun getThreadsByUserId(
            authentication: Authentication
    ): ResponseEntity<Flow<ChatThreadDTO>> {
        val userId = extractUserId(authentication)
            ?: throw IllegalArgumentException("User ID not found in authentication")

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
            authentication: Authentication
    ): ResponseEntity<Void> {
        val userId = extractUserId(authentication)
            ?: throw IllegalArgumentException("User ID not found in authentication")

        chatThreadManagementService.deactivateParticipant(chatRoomId, userId)
        return ResponseEntity.noContent().build()
    }
    
    /**
     * Extract user ID from Authentication object, supporting both JWT and Opaque tokens
     */
    private fun extractUserId(authentication: Authentication): String? {
        return when (val principal = authentication.principal) {
            is Jwt -> {
                logger.debug("Extracting user ID from JWT token")
                // For user tokens, use user_id claim if available, otherwise sub
                principal.claims["user_id"]?.toString() ?: principal.subject
            }
            is OAuth2AuthenticatedPrincipal -> {
                logger.debug("Extracting user ID from Opaque token")
                // For client credentials tokens, sub contains the client ID
                // For user tokens, look for user_id or preferred_username
                val sub = principal.getAttribute<String>("sub")
                val clientId = principal.getAttribute<String>("client_id")
                
                // If sub equals client_id, this is a client credentials token
                if (sub == clientId) {
                    logger.debug("Detected client credentials token, using test user ID")
                    // For testing purposes, use a default test user ID
                    return "test-user-123"
                }
                
                // Otherwise try to get the actual user ID
                principal.getAttribute<String>("user_id") 
                    ?: principal.getAttribute<String>("preferred_username")
                    ?: principal.getAttribute<String>("id")
                    ?: sub
            }
            is String -> {
                logger.debug("Principal is a String: $principal")
                principal
            }
            else -> {
                logger.warn("Unknown principal type: ${principal?.javaClass?.name}")
                null
            }
        }
    }
}
