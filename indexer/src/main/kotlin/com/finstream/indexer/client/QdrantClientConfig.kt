package com.finstream.indexer.client

import io.qdrant.client.QdrantClient
import io.qdrant.client.QdrantGrpcClient
import io.qdrant.client.grpc.Collections.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import jakarta.annotation.PostConstruct
import org.springframework.boot.ApplicationRunner

@Configuration
class QdrantClientConfig(
    @Value("\${qdrant.host:localhost}") private val host: String,
    @Value("\${qdrant.port:6334}") private val port: Int,
    @Value("\${qdrant.api-key:}") private val apiKey: String,
    @Value("\${qdrant.use-tls:false}") private val useTls: Boolean,
    @Value("\${qdrant.collection-name:disclosures}") private val collectionName: String
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Bean
    fun qdrantClient(): QdrantClient {
        val grpcBuilder = QdrantGrpcClient.newBuilder(host, port, useTls)
        if (apiKey.isNotBlank()) {
            grpcBuilder.withApiKey(apiKey)
        }
        return QdrantClient(grpcBuilder.build())
    }

    @Bean
    fun ddlInit(client: QdrantClient) = ApplicationRunner {
        try {
            val collections = client.listCollectionsAsync().get()
            val exists = collections.any { it == collectionName }

            if (!exists) {
                log.info("Creating Qdrant collection: {}", collectionName)
                client.createCollectionAsync(
                    collectionName,
                    VectorParams.newBuilder()
                        .setSize(1536)
                        .setDistance(Distance.Cosine)
                        .build()
                ).get()

                client.createPayloadIndexAsync(
                    collectionName,
                    "stock_code",
                    PayloadSchemaType.Keyword,
                    null, null, null, null
                ).get()

                client.createPayloadIndexAsync(
                    collectionName,
                    "disclosed_at",
                    PayloadSchemaType.Keyword,
                    null, null, null, null
                ).get()

                log.info("Collection '{}' created with payload indexes", collectionName)
            } else {
                log.info("Collection '{}' already exists", collectionName)
            }
        } catch (e: Exception) {
            log.error("Failed to initialize Qdrant collection", e)
            throw RuntimeException("Qdrant initialization failed", e)
        }
    }
}
