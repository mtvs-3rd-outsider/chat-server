package com.example.kotlin.chat.config

import com.example.kotlin.chat.repository.ChatThreadRepository
import com.example.kotlin.chat.repository.ParticipantRepository
import com.example.kotlin.chat.service.*
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MyServiceFactory {

    @Bean
    fun messageThreadListInfoService(
        chatThreadRepository: ChatThreadRepository,
        participantRepository: ParticipantRepository
    ): RealtimeEventService<List<RoomInfoVM>, MessageVM> {
        return MessageThreadListInfoService(chatThreadRepository, participantRepository)
    }

    @Bean
    fun messageTotalUnreadCountService(
        participantRepository: ParticipantRepository,
        userStatusService: UserStatusService
    ): RealtimeEventService<TotalUnreadMessageCountVM, MessageVM> {
        return MessageTotalUnreadCountService(participantRepository, userStatusService)
    }

    @Bean
    fun userLastReadTimeService(
        userStatusService: UserStatusService
    ):  RealtimeEventService<List<UserLastReadTimeVM>, MessageVM> {
        return UserLastReadTimeService(userStatusService)
    }
}
