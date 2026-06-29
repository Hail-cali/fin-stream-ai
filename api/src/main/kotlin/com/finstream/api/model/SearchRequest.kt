package com.finstream.api.model

data class SearchRequest(
    val query: String,
    val stockCode: String? = null,
    val topK: Int = 5
)
