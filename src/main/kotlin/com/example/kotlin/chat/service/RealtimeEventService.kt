package com.example.kotlin.chat.service

import kotlinx.coroutines.flow.Flow

interface RealtimeEventService<T,U> {

    /**
     * 주어진 이벤트 메시지 스트림을 처리하는 함수
     * @param inboundMessages 처리할 메시지의 Flow 스트림
     */
    suspend fun post(inboundMessages: Flow<U>)

    /**
     * 특정 ID에 대한 이벤트 스트림을 반환하는 함수
     * @param id 이벤트를 처리할 대상 ID
     * @return Flow<T> 이벤트 데이터의 스트림
     */
    fun stream(id: String): Flow<T>

    /**
     * 특정 ID에 대한 최신 데이터를 반환하는 함수
     * @param id 이벤트를 처리할 대상 ID
     * @return Flow<T> 최신 이벤트 데이터의 스트림
     */
    suspend fun latest(id: String): Flow<T>
}