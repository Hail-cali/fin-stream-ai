package com.finstream.indexer.client

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class OpenAiEmbeddingClient(
    private val webClient: WebClient,
    private val properties: OpenAiEmbeddingProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun embedBatch(texts: List<String>): List<List<Float>> {
        val allEmbeddings = mutableListOf<List<Float>>()

        texts.chunked(properties.embedding.batchSize).forEach { batch ->
            log.info("Embedding batch of {} texts", batch.size)
            val requestBody = mapOf(
                "model" to properties.embedding.model,
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
