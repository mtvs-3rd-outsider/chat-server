package com.example.kotlin.chat.config

import com.example.kotlin.chat.service.MessageVM
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.ReactiveValueOperations
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext

@Configuration
class RedisConfig {

    @Bean
    fun reactiveRedisTemplate(factory: ReactiveRedisConnectionFactory): ReactiveRedisTemplate<String, MessageVM> {
        // 키 직렬화 설정 (String으로 설정)
        val keySerializer = StringRedisSerializer()

        // 값 직렬화 설정 (MessageVM 객체를 JSON 형식으로 직렬화)
        val valueSerializer = Jackson2JsonRedisSerializer(MessageVM::class.java)

        // 직렬화 컨텍스트 설정
        val serializationContext = RedisSerializationContext
            .newSerializationContext<String, MessageVM>(keySerializer)
            .value(valueSerializer)
            .build()

        // ReactiveRedisTemplate 반환
        return ReactiveRedisTemplate(factory, serializationContext)
    }
    // String -> Boolean 템플릿
    @Bean
    fun booleanReactiveRedisTemplate(factory: ReactiveRedisConnectionFactory): ReactiveRedisTemplate<String, Boolean> {
        val keySerializer = StringRedisSerializer()
        val valueSerializer = Jackson2JsonRedisSerializer(Boolean::class.java)

        val serializationContext = RedisSerializationContext
            .newSerializationContext<String, Boolean>(keySerializer)
            .value(valueSerializer)
            .build()

        return ReactiveRedisTemplate(factory, serializationContext)
    }
    @Bean
    fun stringReactiveRedisTemplate(factory: ReactiveRedisConnectionFactory): ReactiveRedisTemplate<String, String> {
        val serializer = StringRedisSerializer()

        val serializationContext = RedisSerializationContext
            .newSerializationContext<String, String>(serializer)
            .value(serializer)
            .build()

        return ReactiveRedisTemplate(factory, serializationContext)
    }
    // String -> Int 템플릿
    @Bean
    fun intReactiveRedisTemplate(factory: ReactiveRedisConnectionFactory): ReactiveRedisTemplate<String, Int> {
        val keySerializer = StringRedisSerializer()
        val valueSerializer = Jackson2JsonRedisSerializer(Int::class.java) // Int 값 직렬화

        val serializationContext = RedisSerializationContext
            .newSerializationContext<String, Int>(keySerializer)
            .value(valueSerializer)
            .build()

        return ReactiveRedisTemplate(factory, serializationContext)
    }
    @Bean
    fun reactiveRedisMessageListenerContainer(factory: ReactiveRedisConnectionFactory): ReactiveRedisMessageListenerContainer {
        return ReactiveRedisMessageListenerContainer(factory)
    }

    @Bean
    fun messageTopic(): ChannelTopic {
        return ChannelTopic("chatroom") // 사용할 채널 이름을 지정합니다.
    }
}
