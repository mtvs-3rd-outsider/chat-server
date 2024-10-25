package com.example.kotlin.chat.repository


import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.data.repository.query.Param
import reactor.util.function.Tuple2
import java.time.Instant

interface MessageRepository : CoroutineCrudRepository<Message, Int> {

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
    fun findLatestAfterMessageInRoom(roomId: String): Flow<Message>

    // language=SQL
    @Query("""
SELECT * FROM (
    SELECT * FROM MESSAGES
    WHERE ROOM_ID = :roomId AND SENT > (SELECT SENT FROM MESSAGES WHERE ID = :id)
    ORDER BY `SENT` DESC
) AS subquery
ORDER BY `SENT` ASC;
""")
    fun findLatestAfterMessageInRoom(@Param("id") id: Int, @Param("roomId") roomId: String): Flow<Message>


}