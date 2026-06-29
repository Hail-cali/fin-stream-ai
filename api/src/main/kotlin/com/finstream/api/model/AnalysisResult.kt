package com.finstream.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class AnalysisResult(
    @JsonProperty("news_id") val newsId: String,
    @JsonProperty("stock_code") val stockCode: String,
    @JsonProperty("stock_name") val stockName: String,
    @JsonProperty("title") val title: String,
    @JsonProperty("summary") val summary: String?,
    @JsonProperty("sentiment") val sentiment: String?,
    @JsonProperty("sentiment_score") val sentimentScore: Double?,
    @JsonProperty("risk_level") val riskLevel: String?,
    @JsonProperty("risk_reason") val riskReason: String?,
    @JsonProperty("insight") val insight: String?,
    @JsonProperty("rag_sources") val ragSources: List<String>?,
    @JsonProperty("analyzed_at") val analyzedAt: String? = null
)
