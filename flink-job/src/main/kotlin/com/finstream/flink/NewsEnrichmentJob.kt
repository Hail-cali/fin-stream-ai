package com.finstream.flink

import com.finstream.flink.function.DeduplicationFilter
import com.finstream.flink.function.ImportanceScorer
import com.finstream.flink.function.StockCodeExtractor
import com.finstream.flink.model.EnrichedNews
import com.finstream.flink.model.RawNews
import com.finstream.flink.serde.JsonDeserializer
import com.finstream.flink.serde.JsonSerializer
import org.apache.flink.api.common.eventtime.WatermarkStrategy
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema
import org.apache.flink.connector.kafka.sink.KafkaSink
import org.apache.flink.connector.kafka.source.KafkaSource
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment

object NewsEnrichmentJob {

    private const val SOURCE_TOPIC = "raw.news"
    private const val SINK_TOPIC = "enriched.news"
    private const val IMPORTANCE_THRESHOLD = 0.3

    @JvmStatic
    fun main(args: Array<String>) {
        val env = StreamExecutionEnvironment.getExecutionEnvironment()

        // 체크포인트: 5분 간격
        env.enableCheckpointing(300_000)

        val kafkaBootstrap = System.getenv("KAFKA_BOOTSTRAP") ?: "kafka:9092"

        // Kafka Source: raw.news
        val source = KafkaSource.builder<RawNews>()
            .setBootstrapServers(kafkaBootstrap)
            .setTopics(SOURCE_TOPIC)
            .setGroupId("flink-enrichment")
            .setStartingOffsets(OffsetsInitializer.latest())
            .setValueOnlyDeserializer(JsonDeserializer(RawNews::class.java))
            .build()

        // Kafka Sink: enriched.news
        val sink = KafkaSink.builder<EnrichedNews>()
            .setBootstrapServers(kafkaBootstrap)
            .setRecordSerializer(
                KafkaRecordSerializationSchema.builder<EnrichedNews>()
                    .setTopic(SINK_TOPIC)
                    .setValueSerializationSchema(JsonSerializer())
                    .build()
            )
            .build()

        env.fromSource(source, WatermarkStrategy.noWatermarks(), "raw.news")
            // 1. 종목코드 추출/보정
            .map(StockCodeExtractor())
            // TODO(HAIL): 유효한 종목코드가 있는 뉴스만 통과, filter function 으로 리팩토링
            // keyBy 이전에 처리 필요
            .filter { it.stockCode.length == 6 }
            // 2. 중복 제거 (news_id 기준)
            .keyBy { it.newsId }
            .process(DeduplicationFilter())
            // 3. 중요도 스코어링
            .map(ImportanceScorer())
            // 4. 중요도 임계값 이상만 통과
            .filter { it.importanceScore >= IMPORTANCE_THRESHOLD }
            // 5. Kafka Sink
            .sinkTo(sink)

        env.execute("FinStream News Enrichment")
    }
}
