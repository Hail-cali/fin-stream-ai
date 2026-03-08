package com.finstream.indexer.client

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class OpenAiEmbeddingClient(
    @Value("\${openai.api-key}") private val apiKey: String,
    @Value("\${openai.embedding.model:text-embedding-3-small}") private val model: String,
    @Value("\${openai.embedding.batch-size:20}") private val batchSize: Int
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val webClient = WebClient.builder()
        .baseUrl("https://api.openai.com/v1")
        .defaultHeader("Authorization", "Bearer $apiKey")
        .defaultHeader("Content-Type", "application/json")
        .codecs { it.defaultCodecs().maxInMemorySize(10 * 1024 * 1024) }
        .build()

    fun embedBatch(texts: List<String>): List<List<Float>> {
        val allEmbeddings = mutableListOf<List<Float>>()

        texts.chunked(batchSize).forEach { batch ->
            log.info("Embedding batch of {} texts", batch.size)
            val requestBody = mapOf(
                "model" to model,
                "input" to batch
            )

            val response = webClient.post()
                .uri("/embeddings")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(EmbeddingResponse::class.java)
                .block() ?: throw RuntimeException("Failed to get embedding response")

            val sorted = response.data.sortedBy { it.index }
            allEmbeddings.addAll(sorted.map { it.embedding })
        }

        return allEmbeddings
    }

    data class EmbeddingResponse(
        val data: List<EmbeddingData>,
        val model: String,
        val usage: Usage
    )

    data class EmbeddingData(
        val index: Int,
        val embedding: List<Float>
    )

    data class Usage(
        val prompt_tokens: Int,
        val total_tokens: Int
    )
}
