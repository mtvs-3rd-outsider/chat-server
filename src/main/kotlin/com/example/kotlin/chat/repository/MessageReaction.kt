package com.example.kotlin.chat.repository

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table("MESSAGE_REACTIONS")
data class MessageReaction(
    @Id val id: Int?,
    @Column("message_id")
    val messageId: Int,
    @Column("user_id")
    val userId: String,
    @Column("reaction_type")
    val reactionType: String, // 예: "like", "heart", "thumbs_up"
    @Column("room_id")
    val roomId: String
)