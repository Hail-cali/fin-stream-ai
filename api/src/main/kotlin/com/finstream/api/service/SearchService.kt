package com.finstream.api.service

import com.finstream.api.model.SearchResult
import io.qdrant.client.QdrantClient
import io.qdrant.client.grpc.Points
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class SearchService(
    private val qdrantClient: QdrantClient,
    @Value("\${openai.api-key}") private val openaiApiKey: String,
    @Value("\${qdrant.collection-name:disclosures}") private val collectionName: String
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val webClient = WebClient.builder()
        .baseUrl("https://api.openai.com/v1")
        .defaultHeader("Authorization", "Bearer $openaiApiKey")
        .build()

    fun search(query: String, stockCode: String?, topK: Int = 5): List<SearchResult> {
        val embedding = embedQuery(query) ?: return emptyList()

        val searchBuilder = Points.SearchPoints.newBuilder()
            .setCollectionName(collectionName)
            .addAllVector(embedding)
            .setLimit(topK.toLong())
            .setWithPayload(
                Points.WithPayloadSelector.newBuilder().setEnable(true).build()
            )

        // stock_code 필터 (지정 시)
        if (!stockCode.isNullOrBlank()) {
            searchBuilder.setFilter(
                Points.Filter.newBuilder()
                    .addMust(
                        Points.Condition.newBuilder()
                            .setField(
                                Points.FieldCondition.newBuilder()
                                    .setKey("stock_code")
                                    .setMatch(
                                        Points.Match.newBuilder()
                                            .setKeyword(stockCode)
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
        }

        return try {
            val results = qdrantClient.searchAsync(searchBuilder.build()).get()
            results.map { point ->
                val payload = point.payloadMap
                SearchResult(
                    disclosureId = payload["disclosure_id"]?.stringValue ?: "",
                    stockCode = payload["stock_code"]?.stringValue ?: "",
                    title = payload["title"]?.stringValue ?: "",
                    chunkText = payload["chunk_text"]?.stringValue ?: "",
                    score = point.score,
                    disclosedAt = payload["disclosed_at"]?.stringValue
                )
            }
        } catch (e: Exception) {
            log.error("[Search] Qdrant 검색 실패: ${e.message}")
            emptyList()
        }
    }

    private fun embedQuery(text: String): List<Float>? {
        return try {
            val response = webClient.post()
                .uri("/embeddings")
                .bodyValue(
                    mapOf(
                        "model" to "text-embedding-3-small",
                        "input" to text
                    )
                )
                .retrieve()
                .bodyToMono(Map::class.java)
                .block()

            @Suppress("UNCHECKED_CAST")
            val data = response?.get("data") as? List<Map<String, Any>> ?: return null
            val embedding = data.firstOrNull()?.get("embedding") as? List<Double> ?: return null
            embedding.map { it.toFloat() }
        } catch (e: Exception) {
            log.error("[Embedding] OpenAI 임베딩 실패: ${e.message}")
            null
        }
    }
}
