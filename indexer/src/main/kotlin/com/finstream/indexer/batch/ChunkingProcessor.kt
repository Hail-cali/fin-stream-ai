package com.finstream.indexer.batch

import com.finstream.indexer.domain.Disclosure
import com.finstream.indexer.domain.DisclosureChunk
import org.slf4j.LoggerFactory
import org.springframework.batch.item.ItemProcessor
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class ChunkingProcessor(
    @Value("\${indexer.chunk-size:500}") private val chunkSize: Int,
    @Value("\${indexer.chunk-overlap:50}") private val chunkOverlap: Int
) : ItemProcessor<Disclosure, List<DisclosureChunk>> {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun process(disclosure: Disclosure): List<DisclosureChunk> {
        val text = disclosure.content
        val chunks = slidingWindowChunk(text, chunkSize, chunkOverlap)

        log.info(
            "Chunked disclosure '{}' ({}) into {} chunks",
            disclosure.title, disclosure.dartId, chunks.size
        )

        return chunks.mapIndexed { index, chunkText ->
            DisclosureChunk(
                disclosureId = disclosure.dartId,
                stockCode = disclosure.stockCode,
                corpName = disclosure.corpName,
                title = disclosure.title,
                chunkIndex = index,
                totalChunks = chunks.size,
                chunkText = "${disclosure.title}\n\n$chunkText",
                disclosedAt = disclosure.disclosedAt
            )
        }
    }

    private fun slidingWindowChunk(text: String, size: Int, overlap: Int): List<String> {
        if (text.length <= size) return listOf(text)

        val chunks = mutableListOf<String>()
        var start = 0
        while (start < text.length) {
            val end = minOf(start + size, text.length)
            chunks.add(text.substring(start, end))
            if (end == text.length) break
            start += size - overlap
        }
        return chunks
    }
}
