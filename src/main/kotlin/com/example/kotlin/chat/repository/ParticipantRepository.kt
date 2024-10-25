package com.example.kotlin.chat.repository

import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import kotlinx.coroutines.flow.Flow

interface ParticipantRepository : CoroutineCrudRepository<Participant, Long> {
    // 채팅방 ID에 따른 참여자 목록을 Flow로 반환
    fun findByThreadId(threadId: Long): Flow<Participant>

    // 특정 userId에 해당하는 모든 방의 정보를 조회
    fun findByUserId(userId: Long): Flow<Participant>
}
