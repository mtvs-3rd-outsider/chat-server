package com.example.kotlin.chat.repository


import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.data.repository.query.Param
import java.time.Instant

interface MessageReactionRepository : CoroutineCrudRepository<MessageReaction, Int> {

    // 특정 채팅방에서 특정 시간 이전에 발생한 반응을 페이지네이션으로 조회
    @Query("""
        SELECT *
        FROM MESSAGE_REACTIONS
        WHERE ROOM_ID = :roomId AND SENT < :sent
        ORDER BY SENT DESC
        LIMIT :limit
    """)
    fun findReactionsBeforeSent(
        @Param("roomId") roomId: String,
        @Param("sent") sent: Instant, // 기준 시간 (ISO 8601 형식의 시간)
        @Param("limit") limit: Int
    ): Flow<MessageReaction>

        @Query("SELECT * FROM MESSAGE_REACTIONS WHERE ROOM_ID = :roomId")
        fun streamByRoomId(roomId: String): Flow<MessageReaction> // 실시간 반응 스트리밍
}
