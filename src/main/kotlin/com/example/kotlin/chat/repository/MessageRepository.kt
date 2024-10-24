package com.example.kotlin.chat.repository

import com.example.kotlin.chat.service.MessageWithReaction
import com.example.kotlin.chat.service.MessageWithReactionVM
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

//    // 특정 메시지에 대한 모든 반응 조회
//    @Query("""
//        SELECT *
//        FROM MESSAGE_REACTIONS
//        WHERE MESSAGE_ID = :messageId
//    """)
//    fun findReactionsByMessageId(@Param("messageId") messageId: Int): Flow<MessageReaction>
//
//    // 특정 채팅방의 모든 메시지에 대한 반응 조회
//    @Query("""
//        SELECT *
//        FROM MESSAGE_REACTIONS
//        WHERE MESSAGE_ID IN (
//            SELECT ID FROM MESSAGES WHERE ROOM_ID = :roomId
//        )
//    """)
//    fun findReactionsByRoomId(@Param("roomId") roomId: String): Flow<MessageReaction>
//

    // language=SQL
    @Query("""
        SELECT *
        FROM MESSAGES
        WHERE ROOM_ID = :roomId AND SENT < :sent
        ORDER BY SENT DESC
        LIMIT :limit
    """)
    fun findMessagesBeforeSent(
        @Param("roomId") roomId: String,
        @Param("sent") sent: String, // ISO 8601 형식의 날짜 문자열 (Instant로 변환 가능)
        @Param("limit") limit: Int
    ): Flow<Message>

    @Query("""
    SELECT m.*, r.*
    FROM MESSAGES m
    LEFT JOIN MESSAGE_REACTIONS r ON m.ID = r.MESSAGE_ID
    WHERE m.ROOM_ID = :roomId AND m.SENT < :sentTime
    ORDER BY m.SENT DESC
    LIMIT 10
""")
    fun findMessagesWithReactionsBeforeSent(
        @Param("roomId") roomId: String,
        @Param("sentTime") sentTime: Instant
    ): Flow<MessageWithReaction>
}