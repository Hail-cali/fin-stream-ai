package com.finstream.api.model

data class SearchResult(
    val disclosureId: String,
    val stockCode: String,
    val title: String,
    val chunkText: String,
    val score: Float,
    val disclosedAt: String?
)
