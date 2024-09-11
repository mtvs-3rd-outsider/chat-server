package com.example.kotlin.chat.repository

import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.data.repository.query.Param

interface MessageRepository : CoroutineCrudRepository<Message, String> {

    // language=SQL
    @Query("""
SELECT * FROM (
    SELECT * FROM MESSAGES
    WHERE `ROOM_ID` = :roomId
    ORDER BY `SENT` DESC
    LIMIT 10
) AS subquery
ORDER BY `SENT` ASC;
""")
    fun findLatestAfterMessageInRoom(roomId: Int): Flow<Message>

    // language=SQL
    @Query("""
SELECT * FROM (
    SELECT * FROM MESSAGES
    WHERE ROOM_ID = :roomId AND SENT > (SELECT SENT FROM MESSAGES WHERE ID = :id)
    ORDER BY `SENT` DESC
) AS subquery
ORDER BY `SENT` ASC;
""")
    fun findLatestAfterMessageInRoom(@Param("id") id: Int, @Param("roomId") roomId: Int): Flow<Message>
}