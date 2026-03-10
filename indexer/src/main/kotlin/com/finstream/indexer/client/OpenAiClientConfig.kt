package com.finstream.indexer.client

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@EnableConfigurationProperties(OpenAiEmbeddingProperties::class)
class OpenAiClientConfig {

    @Bean
    fun openAiWebClient(properties: OpenAiEmbeddingProperties): WebClient {
        return WebClient.builder()
            .baseUrl(properties.baseUrl)
            .defaultHeader("Authorization", "Bearer ${properties.apiKey}")
            .defaultHeader("Content-Type", "application/json")
            .codecs { it.defaultCodecs().maxInMemorySize(10 * 1024 * 1024) }
            .build()
    }
}
