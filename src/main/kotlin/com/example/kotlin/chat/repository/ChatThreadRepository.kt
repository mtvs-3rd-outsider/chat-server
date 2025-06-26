package com.example.kotlin.chat.repository

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.data.r2dbc.repository.Query
import kotlinx.coroutines.flow.Flow

interface ChatThreadRepository : CoroutineCrudRepository<ChatThread, Long> {
    
    @Query("""
        SELECT ct.* FROM chat_threads ct
        WHERE ct.chat_room_id IN (
            SELECT p1.chat_room_id FROM participants p1
            JOIN participants p2 ON p1.chat_room_id = p2.chat_room_id
            WHERE p1.user_id = :userId1 AND p2.user_id = :userId2
            AND ct.is_group_thread = false
            GROUP BY p1.chat_room_id
            HAVING COUNT(DISTINCT p1.user_id) = 2
        )
    """)
    suspend fun findOneToOneChatByUserIds(userId1: Long, userId2: Long): ChatThread?
    
    @Query("""
        SELECT DISTINCT ct.* FROM chat_threads ct
        JOIN participants p ON ct.chat_room_id = p.chat_room_id
        WHERE p.user_id = :userId
    """)
    fun findThreadsByUserId(userId: Long): Flow<ChatThread>
}
