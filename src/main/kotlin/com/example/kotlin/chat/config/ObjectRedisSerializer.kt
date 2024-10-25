package com.example.kotlin.chat.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.SerializationException

class ObjectRedisSerializer : RedisSerializer<Any> {

    private val objectMapper = ObjectMapper()

    override fun serialize(t: Any?): ByteArray? {
        return try {
            objectMapper.writeValueAsBytes(t)
        } catch (e: Exception) {
            throw SerializationException("Error serializing object", e)
        }
    }

    override fun deserialize(bytes: ByteArray?): Any? {
        return try {
            bytes?.let { objectMapper.readValue(it, Any::class.java) }
        } catch (e: Exception) {
            throw SerializationException("Error deserializing object", e)
        }
    }
}
