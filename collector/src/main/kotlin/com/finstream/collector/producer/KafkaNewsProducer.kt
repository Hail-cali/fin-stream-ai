package com.finstream.collector.producer

import com.finstream.collector.model.RawNews
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class KafkaNewsProducer(
    private val kafkaTemplate: KafkaTemplate<String, RawNews>
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val TOPIC = "raw.news"
    }

    fun send(news: RawNews) {
        kafkaTemplate.send(TOPIC, news.stockCode, news)
            .whenComplete { result, ex ->
                if (ex != null) {
                    log.error("[Kafka] 전송 실패: ${news.newsId} - ${ex.message}")
                } else {
                    val offset = result.recordMetadata.offset()
                    log.info("[Kafka] 전송 완료: ${news.newsId} → partition=${result.recordMetadata.partition()}, offset=$offset")
                }
            }
    }
}
