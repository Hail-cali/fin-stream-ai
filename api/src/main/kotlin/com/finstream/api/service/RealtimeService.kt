package com.finstream.api.service

import com.finstream.api.model.AnalysisResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

@Service
class RealtimeService {

    private val log = LoggerFactory.getLogger(javaClass)
    private val sink: Sinks.Many<AnalysisResult> = Sinks.many().multicast().onBackpressureBuffer(256)

    fun publish(result: AnalysisResult) {
        val emitResult = sink.tryEmitNext(result)
        if (emitResult.isFailure) {
            log.warn("[SSE] Failed to emit: {} — {}", result.newsId, emitResult)
        }
    }

    fun stream(): Flux<AnalysisResult> {
        return sink.asFlux()
    }
}
