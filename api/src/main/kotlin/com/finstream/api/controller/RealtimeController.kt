package com.finstream.api.controller

import com.finstream.api.model.AnalysisResult
import com.finstream.api.service.RealtimeService
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.time.Duration

@RestController
@RequestMapping("/api")
class RealtimeController(
    private val realtimeService: RealtimeService
) {

    @GetMapping("/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun stream(): Flux<ServerSentEvent<AnalysisResult>> {
        // 분석 결과 SSE 스트림 + 30초마다 heartbeat
        val dataStream = realtimeService.stream()
            .map { result ->
                ServerSentEvent.builder(result)
                    .event("analysis")
                    .id(result.newsId)
                    .build()
            }

        val heartbeat = Flux.interval(Duration.ofSeconds(30))
            .map {
                ServerSentEvent.builder<AnalysisResult>()
                    .event("heartbeat")
                    .comment("keep-alive")
                    .build()
            }

        return Flux.merge(dataStream, heartbeat)
    }
}
