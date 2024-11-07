package com.example.kotlin.chat.config

import com.example.kotlin.chat.util.CustomReactiveJwtDecoder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.config.Customizer.withDefaults
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.rsocket.EnableRSocketSecurity
import org.springframework.security.config.annotation.rsocket.RSocketSecurity
import org.springframework.security.config.annotation.rsocket.RSocketSecurity.AuthorizePayloadsSpec
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoders
import org.springframework.security.oauth2.server.resource.authentication.JwtReactiveAuthenticationManager
import org.springframework.security.rsocket.core.PayloadSocketAcceptorInterceptor


@Configuration
@EnableRSocketSecurity
class SecurityConfig(@Value("\${jwt.secret}") private val secretKey: String ) {

    @Bean
    fun authorization(security: RSocketSecurity): PayloadSocketAcceptorInterceptor {
        security.authorizePayload { authorize: AuthorizePayloadsSpec ->
            authorize
                .setup().authenticated()
                .anyRequest().authenticated()
                .anyExchange().authenticated()
        } // all connections, exchanges.
            .jwt(withDefaults())
        return security.build()
    }

        @Bean
    fun reactiveJwtDecoder(): ReactiveJwtDecoder? {
        return CustomReactiveJwtDecoder(secretKey)
    }

    @Bean
    fun jwtAuthenticationManager(reactiveJwtDecoder: ReactiveJwtDecoder): ReactiveAuthenticationManager {
        return JwtReactiveAuthenticationManager(reactiveJwtDecoder)
    }

}

