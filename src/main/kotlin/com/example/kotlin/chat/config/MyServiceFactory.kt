package com.example.kotlin.chat.config

import NoRedisUserStatusService
import RedisUserStatusService
import com.example.kotlin.chat.repository.ChatThreadRepository
import com.example.kotlin.chat.repository.ParticipantRepository
import com.example.kotlin.chat.service.*
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer

@Configuration
class MyServiceFactory {

    @Bean
    @Profile("no_redis")
    fun messageThreadListInfoService(
        chatThreadRepository: ChatThreadRepository,
        participantRepository: ParticipantRepository
    ): RealtimeEventService<List<RoomInfoVM>, MessageVM> {
        return MessageThreadListInfoService(chatThreadRepository, participantRepository)
    }

    @Bean
    fun redisMessageThreadListInfoService(
        chatThreadRepository: ChatThreadRepository,
        participantRepository: ParticipantRepository,
        redisTemplate: ReactiveRedisTemplate<String, String>,
        listenerContainer: ReactiveRedisMessageListenerContainer,
        objectMapper: ObjectMapper,
        @Qualifier("threadListInfo") messageTopic: ChannelTopic
    ): RealtimeEventService<List<RoomInfoVM>, MessageVM> {
        return RedisMessageThreadListInfoService(
            chatThreadRepository,
            participantRepository,
            redisTemplate,
            listenerContainer,
            objectMapper,
            messageTopic
        )
    }
    @Bean
    fun userStatusService(participantRepository: ParticipantRepository, redisTemplate: ReactiveRedisTemplate<String, Boolean>): UserStatusService {
        return RedisUserStatusService(participantRepository,redisTemplate)
    }

    @Bean
    @Profile("no_redis")
    fun noRedisUserStatusService(participantRepository: ParticipantRepository): UserStatusService {
        return NoRedisUserStatusService(participantRepository)
    }
    @Bean("messageTotalUnreadCountService")
    @Profile("no_redis")
    fun messageTotalUnreadCountService(
        participantRepository: ParticipantRepository,
        userStatusService: UserStatusService
    ): RealtimeEventService<TotalUnreadMessageCountVM, MessageVM> {
        return MessageTotalUnreadCountService(participantRepository)
    }
    @Bean("redisMessageTotalUnreadCountService")
    fun redisMessageTotalUnreadCountService(
        participantRepository: ParticipantRepository,
        redisTemplate: ReactiveRedisTemplate<String, String>,
        listenerContainer: ReactiveRedisMessageListenerContainer,
        objectMapper: ObjectMapper,
        @Qualifier("totalUnreadMessageCount") messageTopic: ChannelTopic
    ): RealtimeEventService<TotalUnreadMessageCountVM, MessageVM> {
        return RedisMessageTotalUnreadCountService(
            participantRepository,
            redisTemplate,
            listenerContainer,
            objectMapper,
            messageTopic
        )
    }
    @Bean
    fun redisUserLastReadTimeService(
        userStatusService: UserStatusService,
        redisTemplate: ReactiveRedisTemplate<String, String>,
        listenerContainer: ReactiveRedisMessageListenerContainer,
        objectMapper: ObjectMapper,
        @Qualifier("userLastReadTime") messageTopic: ChannelTopic
    ): RealtimeEventService<List<UserLastReadTimeVM>, MessageVM> {
        return RedisUserLastReadTimeService(
            userStatusService,
            redisTemplate,
            listenerContainer,
            objectMapper,
            messageTopic
        )
    }
}
