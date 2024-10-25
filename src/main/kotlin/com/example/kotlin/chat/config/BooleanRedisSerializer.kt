package com.example.kotlin.chat.config

import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.SerializationException

class BooleanRedisSerializer : RedisSerializer<Boolean> {

    override fun serialize(t: Boolean?): ByteArray {
        return if (t == true) byteArrayOf(1) else byteArrayOf(0)
    }

    override fun deserialize(bytes: ByteArray?): Boolean {
        return bytes?.let { it.isNotEmpty() && it[0].toInt() != 0 } ?: false
    }
}
