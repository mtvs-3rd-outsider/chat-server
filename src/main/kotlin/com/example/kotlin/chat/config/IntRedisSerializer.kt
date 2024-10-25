package com.example.kotlin.chat.config

class IntRedisSerializer : org.springframework.data.redis.serializer.RedisSerializer<Int> {
    override fun serialize(t: Int?): ByteArray? = t?.toString()?.toByteArray()

    override fun deserialize(bytes: ByteArray?): Int? = bytes?.toString(Charsets.UTF_8)?.toIntOrNull()
}