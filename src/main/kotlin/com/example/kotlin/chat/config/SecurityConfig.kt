package com.example.kotlin.chat.config

import com.example.kotlin.chat.security.MultiClientOpaqueTokenIntrospector
import com.example.kotlin.chat.security.JwtOrOpaqueAuthenticationManagerResolver
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer.withDefaults
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.rsocket.EnableRSocketSecurity
import org.springframework.security.config.annotation.rsocket.RSocketSecurity
import org.springframework.security.config.annotation.rsocket.RSocketSecurity.AuthorizePayloadsSpec
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.oauth2.server.resource.introspection.ReactiveOpaqueTokenIntrospector
import org.springframework.security.oauth2.server.resource.introspection.NimbusReactiveOpaqueTokenIntrospector
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtReactiveAuthenticationManager
import org.springframework.security.rsocket.core.PayloadSocketAcceptorInterceptor
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import reactor.core.publisher.Mono
import org.slf4j.LoggerFactory


@Configuration
@EnableRSocketSecurity
@EnableWebFluxSecurity
class SecurityConfig(
    @Value("\${security.oauth2.introspection.endpoint.uri:http://authserver.ugot.svc.cluster.local:80/oauth2/introspect}")
    private val introspectionEndpoint: String,
    @Value("\${security.oauth2.introspection.client-id:internal-introspection-client}")
    private val clientId: String,
    @Value("\${security.oauth2.introspection.client-secret:introspection-secret}")
    private val clientSecret: String,
    @Value("\${security.oauth2.jwt.jwk-set-uri:http://authserver.ugot.svc.cluster.local:80/oauth2/jwks}")
    private val jwkSetUri: String
) {
    companion object {
        private val logger = LoggerFactory.getLogger(SecurityConfig::class.java)
    }
    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        http
            .authorizeExchange {
                it.pathMatchers("/actuator/**").permitAll()  // Actuator 경로만 허용
                    .anyExchange().authenticated()          // 나머지 요청은 인증 필요
            }
            .csrf { it.disable() } // 필요에 따라 CSRF 비활성화
            .oauth2ResourceServer { oauth2 ->
                oauth2.authenticationManagerResolver(jwtOrOpaqueAuthenticationManagerResolver())
                    .authenticationEntryPoint { exchange, ex ->
                        logger.error("Authentication failed: ${ex.message}")
                        val response = exchange.response
                        response.statusCode = org.springframework.http.HttpStatus.UNAUTHORIZED
                        response.headers.add("Content-Type", "application/json")
                        val body = """{"error": "unauthorized", "message": "${ex.message}"}"""
                        val buffer = response.bufferFactory().wrap(body.toByteArray())
                        response.writeWith(reactor.core.publisher.Mono.just(buffer))
                    }
                    .accessDeniedHandler { exchange, denied ->
                        logger.error("Access denied: ${denied.message}")
                        val response = exchange.response
                        response.statusCode = org.springframework.http.HttpStatus.FORBIDDEN
                        response.headers.add("Content-Type", "application/json")
                        val body = """{"error": "access_denied", "message": "${denied.message}"}"""
                        val buffer = response.bufferFactory().wrap(body.toByteArray())
                        response.writeWith(reactor.core.publisher.Mono.just(buffer))
                    }
            }
            .exceptionHandling { exceptions ->
                exceptions.authenticationEntryPoint { exchange, ex ->
                    logger.error("Authentication required: ${ex.message}")
                    val response = exchange.response
                    response.statusCode = org.springframework.http.HttpStatus.UNAUTHORIZED
                    response.headers.add("Content-Type", "application/json")
                    val body = """{"error": "authentication_required", "message": "Authentication is required to access this resource"}"""
                    val buffer = response.bufferFactory().wrap(body.toByteArray())
                    response.writeWith(reactor.core.publisher.Mono.just(buffer))
                }
            }
        return http.build()
    }

    @Bean
    fun authorization(security: RSocketSecurity): PayloadSocketAcceptorInterceptor {
        security.authorizePayload { authorize: AuthorizePayloadsSpec ->
            authorize
                .route("api.v1.status.connect").permitAll()
                .route("api.v1.status.user-counts").permitAll()
                .setup().permitAll()
                .anyRequest().authenticated()
                .anyExchange().authenticated()
        }
            .jwt { jwt ->
                jwt.authenticationManager(JwtReactiveAuthenticationManager(reactiveJwtDecoder()))
            }
        return security.build()
    }

    @Bean
    fun opaqueTokenIntrospector(): ReactiveOpaqueTokenIntrospector {
        println("[Chat Server SecurityConfig] Creating MultiClientOpaqueTokenIntrospector with endpoint: $introspectionEndpoint")
        return MultiClientOpaqueTokenIntrospector(introspectionEndpoint)
    }
    
    @Bean
    fun reactiveJwtDecoder(): ReactiveJwtDecoder {
        println("[Chat Server SecurityConfig] Creating JWT Decoder with JWK Set URI: $jwkSetUri")
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build()
    }
    
    @Bean
    fun jwtOrOpaqueAuthenticationManagerResolver(): JwtOrOpaqueAuthenticationManagerResolver {
        println("[Chat Server SecurityConfig] Creating Hybrid OAuth2 Authentication Manager Resolver")
        return JwtOrOpaqueAuthenticationManagerResolver(
            reactiveJwtDecoder(),
            opaqueTokenIntrospector()
        )
    }
}

