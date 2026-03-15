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
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class QdrantWriter(
    private val qdrantClient: QdrantClient,
) : ItemWriter<List<EmbeddedChunk>> {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun write(chunk: Chunk<out List<EmbeddedChunk>>) {
        val allChunks = chunk.items.flatten()
        if (allChunks.isEmpty()) return
        val collectionName = allChunks.first().collectionName

        val points = allChunks.map { embeddedChunk ->
            val uuid = UUID.nameUUIDFromBytes(
                "${embeddedChunk.disclosureId}-${embeddedChunk.chunkIndex}".toByteArray()
            )

            PointStruct.newBuilder()
                .setId(pointId(uuid))
                .setVectors(qdrantVectors(embeddedChunk.embedding))
                .putAllPayload(
                    mapOf(
                        EmbeddedChunk::disclosureId.name to qdrantValue(embeddedChunk.disclosureId),
                        EmbeddedChunk::stockCode.name to qdrantValue(embeddedChunk.stockCode),
                        EmbeddedChunk::corpName.name to qdrantValue(embeddedChunk.corpName),
                        EmbeddedChunk::title.name to qdrantValue(embeddedChunk.title),
                        EmbeddedChunk::chunkIndex.name to qdrantValue(embeddedChunk.chunkIndex.toLong()),
                        EmbeddedChunk::totalChunks.name to qdrantValue(embeddedChunk.totalChunks.toLong()),
                        EmbeddedChunk::chunkText.name to qdrantValue(embeddedChunk.chunkText),
                        EmbeddedChunk::disclosedAt.name to qdrantValue(embeddedChunk.disclosedAt.toString())
                    )
                )
                .build()
        }

        log.info("Upserting {} points to Qdrant collection '{}'", points.size, collectionName)
        qdrantClient.upsertAsync(collectionName, points).get()
        log.info("Successfully upserted {} points", points.size)
    }
}
