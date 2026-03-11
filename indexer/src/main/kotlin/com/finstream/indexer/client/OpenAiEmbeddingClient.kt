package com.finstream.indexer.client

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

@Component
class OpenAiEmbeddingClient(
    private val webClient: WebClient,
    private val properties: OpenAiEmbeddingProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun embedBatch(texts: List<String>): Mono<List<List<Float>>> {
        if (texts.isEmpty()) return Mono.just(emptyList())

        return Flux.fromIterable(texts.chunked(properties.embedding.batchSize))
            .concatMap { batch ->
                log.info("Embedding batch of {} texts", batch.size)
                val requestBody = mapOf(
                    "model" to properties.embedding.model,
                    "input" to batch
                )

                webClient.post()
                    .uri("/embeddings")
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus({ it.is4xxClientError || it.is5xxServerError }) { response ->
                        response.bodyToMono(String::class.java)
                            .defaultIfEmpty("No error body")
                            .flatMap { errorBody ->
                                Mono.error(OpenAiApiException.from(response.statusCode().value(), errorBody))
                            }
                    }
                    .bodyToMono(EmbeddingResponse::class.java)
                    .timeout(Duration.ofSeconds(30))
                    .onErrorMap(WebClientResponseException.TooManyRequests::class.java) {
                        OpenAiApiException("OpenAI rate limit exceeded (429). Back off and retry later.")
                    }
                    .onErrorMap(WebClientResponseException.Unauthorized::class.java) {
                        OpenAiApiException("OpenAI authentication failed (401). Check API key.")
                    }
                    .onErrorMap(WebClientResponseException.BadRequest::class.java) {
                        OpenAiApiException("OpenAI request validation failed (400). Check model/input format.")
                    }
                    .map { response ->
                        response.data
                            .sortedBy { it.index }
                            .map { it.embedding }
                    }
            }
            .collectList()
            .map { embeddingLists -> embeddingLists.flatten() }
    }

    class OpenAiApiException(message: String) : RuntimeException(message) {
        companion object {
            fun from(statusCode: Int, body: String): OpenAiApiException {
                val message = when (statusCode) {
                    400 -> "OpenAI request validation failed (400). Check model/input format. body=$body"
                    401 -> "OpenAI authentication failed (401). Check API key. body=$body"
                    403 -> "OpenAI authorization failed (403). Check account permissions. body=$body"
                    404 -> "OpenAI endpoint not found (404). Check base URL/path. body=$body"
                    408 -> "OpenAI request timeout (408). Retry with smaller batch. body=$body"
                    409 -> "OpenAI conflict (409). Retry request. body=$body"
                    429 -> "OpenAI rate limit exceeded (429). Back off and retry later. body=$body"
                    in 500..599 -> "OpenAI server error ($statusCode). Retry later. body=$body"
                    else -> "OpenAI API error ($statusCode). body=$body"
                }
                return OpenAiApiException(message)
            }
        }
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
