package com.finstream.api.config

import io.qdrant.client.QdrantClient
import io.qdrant.client.QdrantGrpcClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class QdrantClientConfig(
    @Value("\${qdrant.host:localhost}") private val host: String,
    @Value("\${qdrant.port:6334}") private val port: Int
) {

    @Bean
    fun qdrantClient(): QdrantClient {
        val grpcClient = QdrantGrpcClient.newBuilder(host, port, false).build()
        return QdrantClient(grpcClient)
    }
}
