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
        userStatusService: UserStatusService,
        objectMapper: ObjectMapper,
        @Qualifier("threadListInfo") messageTopic: ChannelTopic
    ): RealtimeEventService<Map<String, RoomInfoVM>, MessageVM> {
        return RedisMessageThreadListInfoService(
            chatThreadRepository,
            participantRepository,
            redisTemplate,
            listenerContainer,
            objectMapper,
            userStatusService,
            messageTopic
        )
    }
    @Bean
    fun userStatusService(
        @Qualifier("threadListInfo") threadListInfoTopic: ChannelTopic
        ,@Qualifier("userLastReadTime")  userLastReadTimeTopic: ChannelTopic,
        stringRedisTemplate: ReactiveRedisTemplate<String, String>,
        objectMapper: ObjectMapper,
        participantRepository: ParticipantRepository, redisTemplate: ReactiveRedisTemplate<String, Boolean>): UserStatusService {
        return RedisUserStatusService(participantRepository,redisTemplate,stringRedisTemplate,threadListInfoTopic,userLastReadTimeTopic,objectMapper)
    }

    @Bean
    @Profile("no_redis")
    fun noRedisUserStatusService(participantRepository: ParticipantRepository): UserStatusService {
        return NoRedisUserStatusService(participantRepository)
    }

    @Bean("redisMessageTotalUnreadCountService")
    fun redisMessageTotalUnreadCountService(
        participantRepository: ParticipantRepository,
        redisTemplate: ReactiveRedisTemplate<String, String>,
        listenerContainer: ReactiveRedisMessageListenerContainer,
        objectMapper: ObjectMapper,
        userStatusService: UserStatusService,
        @Qualifier("totalUnreadMessageCount") messageTopic: ChannelTopic
    ): RealtimeEventService<TotalUnreadMessageCountVM, MessageVM> {
        return RedisMessageTotalUnreadCountService(
            participantRepository,
            redisTemplate,
            listenerContainer,
            objectMapper,
            userStatusService,
            messageTopic
        )
    }
    @Bean
    fun redisUserLastReadTimeService(
        userStatusService: UserStatusService,
        redisTemplate: ReactiveRedisTemplate<String, String>,
        listenerContainer: ReactiveRedisMessageListenerContainer,
        objectMapper: ObjectMapper,
        participantRepository: ParticipantRepository,
        @Qualifier("userLastReadTime") messageTopic: ChannelTopic
    ): RealtimeEventService<Map<String, UserLastReadTimeVM>, MessageVM> {
        return RedisUserLastReadTimeService(
            userStatusService,
            redisTemplate,
            listenerContainer,
            objectMapper,
            messageTopic,
            participantRepository,
        )
    }
}
