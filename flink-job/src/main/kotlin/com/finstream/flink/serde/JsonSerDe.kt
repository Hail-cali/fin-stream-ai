package com.finstream.flink.serde

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.flink.api.common.serialization.DeserializationSchema
import org.apache.flink.api.common.serialization.SerializationSchema
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.java.typeutils.TypeExtractor

class JsonDeserializer<T>(private val clazz: Class<T>) : DeserializationSchema<T> {

    @Transient
    private var mapper: ObjectMapper? = null

    private fun getMapper(): ObjectMapper {
        if (mapper == null) {
            mapper = jacksonObjectMapper().apply {
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }
        }
        return mapper!!
    }

    override fun deserialize(message: ByteArray): T {
        return getMapper().readValue(message, clazz)
    }

    override fun isEndOfStream(nextElement: T): Boolean = false

    override fun getProducedType(): TypeInformation<T> {
        return TypeExtractor.getForClass(clazz)
    }
}

class JsonSerializer<T> : SerializationSchema<T> {

    @Transient
    private var mapper: ObjectMapper? = null

    private fun getMapper(): ObjectMapper {
        if (mapper == null) {
            mapper = jacksonObjectMapper()
        }
        return mapper!!
    }

    override fun serialize(element: T): ByteArray {
        return getMapper().writeValueAsBytes(element)
    }
}
