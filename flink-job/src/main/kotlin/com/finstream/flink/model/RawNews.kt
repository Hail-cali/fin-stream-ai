package com.finstream.flink.model

import com.fasterxml.jackson.annotation.JsonProperty

data class RawNews(
    @JsonProperty("news_id") val newsId: String = "",
    @JsonProperty("stock_code") val stockCode: String = "",
    @JsonProperty("stock_name") val stockName: String = "",
    @JsonProperty("title") val title: String = "",
    @JsonProperty("content") val content: String = "",
    @JsonProperty("source") val source: String = "",
    @JsonProperty("published_at") val publishedAt: String = ""
)
