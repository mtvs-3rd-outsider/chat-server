package com.example.kotlin.chat.security

import org.slf4j.LoggerFactory
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.ReactiveAuthenticationManagerResolver
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtReactiveAuthenticationManager
import org.springframework.security.oauth2.server.resource.authentication.OpaqueTokenReactiveAuthenticationManager
import org.springframework.security.oauth2.server.resource.introspection.ReactiveOpaqueTokenIntrospector
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.util.Base64

/**
 * JWT와 Opaque 토큰을 자동으로 감지하여 적절한 인증 매니저로 라우팅하는 리졸버
 * AIServer의 구현을 참고하여 Chat Server에 맞게 구현
 */
class JwtOrOpaqueAuthenticationManagerResolver(
    private val jwtDecoder: ReactiveJwtDecoder,
    private val opaqueTokenIntrospector: ReactiveOpaqueTokenIntrospector
) : ReactiveAuthenticationManagerResolver<ServerWebExchange> {
    
    companion object {
        private val logger = LoggerFactory.getLogger(JwtOrOpaqueAuthenticationManagerResolver::class.java)
    }
    
    private val jwtAuthenticationManager = JwtReactiveAuthenticationManager(jwtDecoder)
    private val opaqueTokenAuthenticationManager = OpaqueTokenReactiveAuthenticationManager(opaqueTokenIntrospector)
    
    override fun resolve(exchange: ServerWebExchange): Mono<ReactiveAuthenticationManager> {
        return extractBearerToken(exchange)
            .map { token ->
                when {
                    isJwtToken(token) -> {
                        logger.debug("Detected JWT token")
                        jwtAuthenticationManager
                    }
                    else -> {
                        logger.debug("Detected Opaque token")
                        opaqueTokenAuthenticationManager
                    }
                }
            }
            .defaultIfEmpty(opaqueTokenAuthenticationManager) // 기본값은 Opaque 토큰
    }
    
    private fun extractBearerToken(exchange: ServerWebExchange): Mono<String> {
        val authorization = exchange.request.headers.getFirst("Authorization") ?: return Mono.empty()
        
        return if (authorization.startsWith("Bearer ", ignoreCase = true)) {
            Mono.just(authorization.substring(7))
        } else {
            Mono.empty()
        }
    }
    
    private fun isJwtToken(token: String): Boolean {
        // JWT는 3개의 파트로 구성되며 '.'으로 구분됨
        val parts = token.split(".")
        if (parts.size != 3) {
            return false
        }
        
        return try {
            // 헤더 파트를 디코드하여 JWT 형식인지 확인
            val headerJson = String(Base64.getUrlDecoder().decode(parts[0]))
            // typ이 JWT이거나 alg 필드가 있으면 JWT로 간주
            headerJson.contains("\"typ\":\"JWT\"") || 
            headerJson.contains("\"alg\":") ||
            headerJson.contains("\"typ\": \"JWT\"") ||
            headerJson.contains("\"alg\": ")
        } catch (e: Exception) {
            logger.debug("Failed to decode token header: ${e.message}")
            false
        }
    }
}