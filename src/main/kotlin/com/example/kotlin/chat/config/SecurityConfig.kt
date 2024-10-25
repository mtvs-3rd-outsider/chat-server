package com.example.kotlin.chat.config

import com.example.kotlin.chat.util.CustomReactiveJwtDecoder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.config.annotation.rsocket.EnableRSocketSecurity
import org.springframework.security.config.annotation.rsocket.RSocketSecurity
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtReactiveAuthenticationManager
import org.springframework.security.rsocket.core.PayloadSocketAcceptorInterceptor

@Configuration
@EnableRSocketSecurity
class SecurityConfig(@Value("\${jwt.secret}") private val secretKey: String) {

    @Bean
    fun rsocketInterceptor(
        rsocket: RSocketSecurity,
        reactiveAuthenticationManager: ReactiveAuthenticationManager
    ): PayloadSocketAcceptorInterceptor {
        rsocket
            .authorizePayload { authorize ->
                authorize
                    .setup().permitAll()     // 연결 설정 단계에서 인증 요구
                    .anyRequest().permitAll() // 모든 요청에 대해 인증 요구
                    .anyExchange().permitAll() // 모든 요청에 대해 인증 요구
            }
            .jwt { jwtSpec ->
                jwtSpec.authenticationManager(reactiveAuthenticationManager)
            }
        return rsocket.build()
    }

    @Bean
    fun reactiveJwtDecoder(): ReactiveJwtDecoder {
        println("TEST reactive")
        return CustomReactiveJwtDecoder(secretKey)
    }

    @Bean
    fun reactiveAuthenticationManager(reactiveJwtDecoder: ReactiveJwtDecoder): ReactiveAuthenticationManager {
        return JwtReactiveAuthenticationManager(reactiveJwtDecoder)
    }
}


//package com.example.kotlin.chat.config
//
//
//import com.example.kotlin.chat.util.CustomReactiveJwtDecoder
//import org.springframework.beans.factory.annotation.Value
//import org.springframework.context.annotation.Bean
//import org.springframework.context.annotation.Configuration
//import org.springframework.security.authentication.ReactiveAuthenticationManager
//import org.springframework.security.config.annotation.rsocket.EnableRSocketSecurity
//import org.springframework.security.config.annotation.rsocket.RSocketSecurity
//import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
//import org.springframework.security.oauth2.server.resource.authentication.JwtReactiveAuthenticationManager
//import org.springframework.security.rsocket.core.PayloadSocketAcceptorInterceptor
//
//
//@Configuration
//@EnableRSocketSecurity
//class SecurityConfig( @Value("\${jwt.secret}") private val secretKey: String) {
//
//
//
//
//    @Bean
//    fun rsocketInterceptor(rsocket: RSocketSecurity, jwtDecoder: ReactiveJwtDecoder): PayloadSocketAcceptorInterceptor {
//        rsocket
//            .authorizePayload { authorize -> authorize
//                .setup().authenticated()
//                .anyRequest().permitAll()
//            }
//            .jwt { jwtSpec ->
//                jwtSpec.decoder(reactiveJwtDecoder()) // 커스텀 디코더 직접 지정
//            }
//        return rsocket.build()
//    }
//
//    @Bean
//    fun reactiveJwtDecoder(): ReactiveJwtDecoder {
//        println("TEST reactive");
//        return CustomReactiveJwtDecoder(secretKey)
//    }
//
//}
//
////    @Bean
////    open fun userDetailsService(): MapReactiveUserDetailsService {
////        val user = User.withDefaultPasswordEncoder()
////            .username("user")
////            .password("user")
////            .roles("USER")
////            .build()
////        return MapReactiveUserDetailsService(user)
////    }
