package com.finstream.indexer.client

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "openai")
data class OpenAiEmbeddingProperties(
    val apiKey: String,
    val baseUrl: String = "https://api.openai.com/v1",
    val embedding: Embedding = Embedding()
) {
    data class Embedding(
        val model: String = "text-embedding-3-small",
        val batchSize: Int = 20
    )
}
