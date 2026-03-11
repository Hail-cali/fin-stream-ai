package com.finstream.indexer.batch

import com.finstream.indexer.client.OpenAiEmbeddingClient
import com.finstream.indexer.domain.DisclosureChunk
import com.finstream.indexer.domain.EmbeddedChunk
import org.slf4j.LoggerFactory
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

@Component
class EmbeddingProcessor(
    private val openAiEmbeddingClient: OpenAiEmbeddingClient
) : ItemProcessor<List<DisclosureChunk>, List<EmbeddedChunk>> {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun process(chunks: List<DisclosureChunk>): List<EmbeddedChunk> {
        if (chunks.isEmpty()) return emptyList()

        val texts = chunks.map { it.chunkText }
        log.info("Embedding {} chunks for disclosure '{}'", chunks.size, chunks.first().title)

        val embeddings = openAiEmbeddingClient.embedBatch(texts).block()
            ?: throw IllegalStateException("Failed to receive embeddings from OpenAI")
        log.info("Received {} embeddings for disclosure '{}'", embeddings.size, chunks.first().title)
        require(embeddings.size == chunks.size) {
            "Embedding count (${embeddings.size}) does not match chunk count (${chunks.size}) for disclosure ${chunks.first().disclosureId}"
        }

        return chunks.zip(embeddings).map { (chunk, embedding) ->
            EmbeddedChunk(
                disclosureId = chunk.disclosureId,
                stockCode = chunk.stockCode,
                corpName = chunk.corpName,
                title = chunk.title,
                chunkIndex = chunk.chunkIndex,
                totalChunks = chunk.totalChunks,
                chunkText = chunk.chunkText,
                disclosedAt = chunk.disclosedAt,
                embedding = embedding
            )
        }
    }
}
