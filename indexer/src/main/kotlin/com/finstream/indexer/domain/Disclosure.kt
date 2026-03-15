package com.finstream.indexer.domain

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.OffsetDateTime

data class Disclosure(
    @JsonProperty("dartId") val dartId: String,
    @JsonProperty("stockCode") val stockCode: String,
    @JsonProperty("corpName") val corpName: String,
    @JsonProperty("title") val title: String,
    @JsonProperty("content") val content: String,
    @JsonProperty("disclosedAt") val disclosedAt: OffsetDateTime
)

data class DisclosureChunk(
    val disclosureId: String,
    val stockCode: String,
    val corpName: String,
    val title: String,
    val chunkIndex: Int,
    val totalChunks: Int,
    val chunkText: String,
    val disclosedAt: OffsetDateTime
)

data class EmbeddedChunk(
    val collectionName: String,
    val disclosureId: String,
    val stockCode: String,
    val corpName: String,
    val title: String,
    val chunkIndex: Int,
    val totalChunks: Int,
    val chunkText: String,
    val disclosedAt: OffsetDateTime,
    val embedding: List<Float>,
)
