package com.finstream.api.kafka

import com.finstream.api.model.AnalysisResult
import com.finstream.api.service.AnalysisService
import com.finstream.api.service.RealtimeService
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class AnalyzedNewsConsumer(
    private val analysisService: AnalysisService,
    private val realtimeService: RealtimeService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = ["analyzed.news"], groupId = "api-server")
    fun consume(result: AnalysisResult) {
        log.info("[Kafka] 분석 결과 수신: ${result.newsId} (${result.stockName})")

        // in-memory 저장
        analysisService.save(result)

        // SSE 브로드캐스트
        realtimeService.publish(result)
    }
}
