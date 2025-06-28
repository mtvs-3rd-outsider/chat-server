package com.example.kotlin.chat.security

import org.slf4j.LoggerFactory
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtReactiveAuthenticationManager
import org.springframework.security.oauth2.server.resource.authentication.OpaqueTokenReactiveAuthenticationManager
import org.springframework.security.oauth2.server.resource.introspection.ReactiveOpaqueTokenIntrospector
import reactor.core.publisher.Mono
import java.util.Base64

/**
 * Chat Server용 JWT 또는 Opaque 토큰을 자동으로 감지하고 적절한 AuthenticationManager를 선택하는 리졸버
 * AuthServer와 AiServer의 하이브리드 방식을 참고하여 구현
 */
class ChatServerJwtOrOpaqueAuthenticationManagerResolver(
    private val jwtDecoder: ReactiveJwtDecoder,
    private val opaqueTokenIntrospector: ReactiveOpaqueTokenIntrospector
) : ReactiveAuthenticationManager {

    companion object {
        private val logger = LoggerFactory.getLogger(ChatServerJwtOrOpaqueAuthenticationManagerResolver::class.java)
    }

    private val jwtAuthenticationManager = JwtReactiveAuthenticationManager(jwtDecoder)
    private val opaqueTokenAuthenticationManager = OpaqueTokenReactiveAuthenticationManager(opaqueTokenIntrospector)

    override fun authenticate(authentication: org.springframework.security.core.Authentication): Mono<org.springframework.security.core.Authentication> {
        val token = authentication.credentials as? String
        
        if (token == null) {
            logger.debug("No token found in authentication")
            return Mono.error(IllegalArgumentException("Token is required"))
        }

        logger.debug("Processing token: ${token.take(20)}...")

        return if (isJwtToken(token)) {
            logger.debug("Detected JWT token, using JWT authentication manager")
            jwtAuthenticationManager.authenticate(authentication)
        } else {
            logger.debug("Detected opaque token, using opaque token authentication manager")
            opaqueTokenAuthenticationManager.authenticate(authentication)
        }
    }

    /**
     * JWT 토큰인지 확인하는 메서드
     * AuthServer의 로직을 참고하여 구현
     */
    private fun isJwtToken(token: String): Boolean {
        try {
            // 1. JWT는 3개의 part로 구성됨 (header.payload.signature)
            val parts = token.split(".")
            if (parts.size != 3) {
                logger.debug("Token does not have 3 parts, treating as opaque token")
                return false
            }

            // 2. Base64URL 디코딩 시도
            val header = try {
                String(Base64.getUrlDecoder().decode(parts[0]))
            } catch (e: IllegalArgumentException) {
                logger.debug("Failed to decode header as Base64URL, treating as opaque token")
                return false
            }

            // 3. 헤더에 JWT 관련 필드가 있는지 확인
            val isJwt = header.contains("\"typ\"") && header.contains("\"JWT\"") ||
                       header.contains("\"alg\"")

            logger.debug("Header analysis result: isJWT = $isJwt, header = $header")
            return isJwt

        } catch (e: Exception) {
            logger.debug("Exception during JWT token detection: ${e.message}, treating as opaque token")
            return false
        }
    }

}