package com.example.kotlin.chat.security

import org.slf4j.LoggerFactory
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal
import org.springframework.security.oauth2.server.resource.introspection.NimbusReactiveOpaqueTokenIntrospector
import org.springframework.security.oauth2.server.resource.introspection.ReactiveOpaqueTokenIntrospector
import reactor.core.publisher.Mono

/**
 * 여러 클라이언트 자격 증명으로 토큰 인트로스펙션을 시도하는 커스텀 인트로스펙터
 * test-init-client와 internal-introspection-client를 모두 지원
 */
class MultiClientOpaqueTokenIntrospector(
    private val introspectionEndpoint: String
) : ReactiveOpaqueTokenIntrospector {
    
    companion object {
        private val logger = LoggerFactory.getLogger(MultiClientOpaqueTokenIntrospector::class.java)
    }
    
    // 여러 클라이언트 자격 증명 목록
    private val clientCredentials = listOf(
        Pair("internal-introspection-client", "introspection-secret"),
        Pair("test-init-client", "test-secret-2024")
    )
    
    private val introspectors = clientCredentials.map { (clientId, clientSecret) ->
        clientId to NimbusReactiveOpaqueTokenIntrospector(introspectionEndpoint, clientId, clientSecret)
    }.toMap()

    override fun introspect(token: String): Mono<OAuth2AuthenticatedPrincipal> {
        logger.debug("Attempting token introspection with multiple client credentials")
        
        // 첫 번째 클라이언트부터 순차적으로 시도
        return tryIntrospectWithClient(token, 0)
    }
    
    private fun tryIntrospectWithClient(token: String, clientIndex: Int): Mono<OAuth2AuthenticatedPrincipal> {
        if (clientIndex >= clientCredentials.size) {
            logger.warn("All client credentials failed for token introspection")
            return Mono.error(IllegalArgumentException("Token introspection failed with all client credentials"))
        }
        
        val (clientId, _) = clientCredentials[clientIndex]
        val introspector = introspectors[clientId]!!
        
        logger.debug("Trying token introspection with client: $clientId")
        
        return introspector.introspect(token)
            .doOnSuccess { principal ->
                logger.info("Token introspection successful with client: $clientId, principal: ${principal.name}")
            }
            .doOnError { error ->
                logger.debug("Token introspection failed with client: $clientId, error: ${error.message}")
            }
            .onErrorResume { error ->
                logger.debug("Falling back to next client after error with $clientId: ${error.message}")
                // 다음 클라이언트로 시도
                tryIntrospectWithClient(token, clientIndex + 1)
            }
    }
}