package com.finstream.indexer.batch

import com.finstream.indexer.domain.EmbeddedChunk
import io.qdrant.client.QdrantClient
import io.qdrant.client.grpc.Points.*
import io.qdrant.client.PointIdFactory.id as pointId
import io.qdrant.client.ValueFactory.value as qdrantValue
import io.qdrant.client.VectorsFactory.vectors as qdrantVectors
import org.slf4j.LoggerFactory
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class QdrantWriter(
    private val qdrantClient: QdrantClient,
    @Value("\${qdrant.collection-name:disclosures}") private val collectionName: String
) : ItemWriter<List<EmbeddedChunk>> {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun write(chunk: Chunk<out List<EmbeddedChunk>>) {
        val allChunks = chunk.items.flatten()
        if (allChunks.isEmpty()) return

        val points = allChunks.map { embeddedChunk ->
            val uuid = UUID.nameUUIDFromBytes(
                "${embeddedChunk.disclosureId}-${embeddedChunk.chunkIndex}".toByteArray()
            )

            PointStruct.newBuilder()
                .setId(pointId(uuid))
                .setVectors(qdrantVectors(embeddedChunk.embedding))
                .putAllPayload(
                    mapOf(
                        "disclosure_id" to qdrantValue(embeddedChunk.disclosureId),
                        "stock_code" to qdrantValue(embeddedChunk.stockCode),
                        "corp_name" to qdrantValue(embeddedChunk.corpName),
                        "title" to qdrantValue(embeddedChunk.title),
                        "chunk_index" to qdrantValue(embeddedChunk.chunkIndex.toLong()),
                        "total_chunks" to qdrantValue(embeddedChunk.totalChunks.toLong()),
                        "chunk_text" to qdrantValue(embeddedChunk.chunkText),
                        "disclosed_at" to qdrantValue(embeddedChunk.disclosedAt.toString())
                    )
                )
                .build()
        }

        log.info("Upserting {} points to Qdrant collection '{}'", points.size, collectionName)
        qdrantClient.upsertAsync(collectionName, points).get()
        log.info("Successfully upserted {} points", points.size)
    }
}
