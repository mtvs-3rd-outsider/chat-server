package com.example.kotlin.chat.util

import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import reactor.core.publisher.Mono
import java.security.Key

class CustomReactiveJwtDecoder(secretKey: String) : ReactiveJwtDecoder {

    private val key: Key

    init {
        println("Initializing CustomReactiveJwtDecoder with secret key")
        val keyBytes = Decoders.BASE64.decode(secretKey)
        this.key = Keys.hmacShaKeyFor(keyBytes)
    }

    override fun decode(token: String): Mono<Jwt> {
        return Mono.fromCallable {
            try {
                println("Attempting to decode JWT token: $token")

                // JWT 파싱 및 검증
                val claimsJws = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)

                println("JWT token successfully parsed. Extracting claims...")

                val body: Claims = claimsJws.body

                println("JWT claims: $body")
                println("JWT headers: ${claimsJws.header}")

                Jwt(
                    token,
                    body.issuedAt.toInstant(),
                    body.expiration.toInstant(),
                    claimsJws.header,
                    body
                ).also {
                    println("JWT object created successfully: $it")
                }
            } catch (e: Exception) {
                println("Failed to decode JWT token: ${e.message}")
                throw JwtException("Invalid JWT token", e)
            }
        }
    }
}

//package com.example.kotlin.chat.util
//
//import io.jsonwebtoken.Claims
//import io.jsonwebtoken.JwtException
//import io.jsonwebtoken.Jwts
//import io.jsonwebtoken.io.Decoders
//import io.jsonwebtoken.security.Keys
//import org.springframework.security.oauth2.jwt.Jwt
//import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
//import reactor.core.publisher.Mono
//import java.security.Key
//
//class CustomReactiveJwtDecoder(secretKey: String) : ReactiveJwtDecoder {
//
//    private val key: Key
//
//    init {
//        val keyBytes = Decoders.BASE64.decode(secretKey)
//        this.key = Keys.hmacShaKeyFor(keyBytes)
//    }
//
//    override fun decode(token: String): Mono<Jwt> {
//        return Mono.fromCallable {
//            try {
//                println("TEST")
//                val claimsJws = Jwts.parserBuilder()
//                    .setSigningKey(key)
//                    .build()
//                    .parseClaimsJws(token)
//
//                val body: Claims = claimsJws.body
//
//                Jwt(
//                    token,
//                    body.issuedAt.toInstant(),
//                    body.expiration.toInstant(),
//                    claimsJws.header,
//                    body
//                )
//            } catch (e: Exception) {
//                throw JwtException("Invalid JWT token", e) // 예외 변환
//            }
//        }
//    }
//}
