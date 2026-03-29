package com.finstream.collector.source

import com.finstream.collector.model.RawNews
import com.finstream.collector.producer.KafkaNewsProducer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@Component
class DartCollector(
    private val producer: KafkaNewsProducer,
    @Value("\${collector.dart.api-key:}") private val apiKey: String,
    @Value("\${collector.dart.enabled:true}") private val enabled: Boolean
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val webClient = WebClient.builder()
        .baseUrl("https://opendart.fss.or.kr/api")
        .build()

    // 종목코드 → 종목명 매핑 (MVP: 주요 종목)
    private val stockNameMap = mapOf(
        "005930" to "삼성전자",
        "000660" to "SK하이닉스",
        "035420" to "NAVER",
        "035720" to "카카오",
        "005380" to "현대자동차",
        "006400" to "삼성SDI",
        "051910" to "LG화학",
        "003670" to "포스코홀딩스",
        "105560" to "KB금융",
        "055550" to "신한지주"
    )

    // 이미 처리한 공시 ID 추적 (in-memory, 서버 재시작 시 초기화)
    private val processedIds = Collections.synchronizedSet(mutableSetOf<String>())

    @Scheduled(fixedDelay = 600_000, initialDelay = 5_000) // 10분 주기, 시작 5초 후
    fun collect() {
        if (!enabled || apiKey.isBlank()) {
            log.debug("[DART] 비활성화 또는 API 키 미설정")
            return
        }

        val today = LocalDate.now(ZoneId.of("Asia/Seoul"))
        val dateStr = today.format(DateTimeFormatter.BASIC_ISO_DATE) // yyyyMMdd

        try {
            val response = webClient.get()
                .uri { uri ->
                    uri.path("/list.json")
                        .queryParam("crtfc_key", apiKey)
                        .queryParam("bgn_de", dateStr)
                        .queryParam("end_de", dateStr)
                        .queryParam("page_count", 100)
                        .build()
                }
                .retrieve()
                .bodyToMono(DartListResponse::class.java)
                .timeout(java.time.Duration.ofSeconds(30))
                .block()

            if (response == null || response.status != "000") {
                log.warn("[DART] API 응답 실패: status=${response?.status}, message=${response?.message}")
                return
            }

            val disclosures = response.list ?: emptyList()
            log.info("[DART] 오늘($dateStr) 공시 ${disclosures.size}건 조회")

            var sent = 0
            for (item in disclosures) {
                if (item.rceptNo in processedIds) continue

                val stockCode = item.stockCode?.padStart(6, '0') ?: continue
                val stockName = stockNameMap[stockCode] ?: item.corpName ?: continue

                val news = RawNews(
                    newsId = "dart-${item.rceptNo}",
                    stockCode = stockCode,
                    stockName = stockName,
                    title = item.reportNm ?: "공시",
                    content = buildDartContent(item),
                    source = "DART",
                    publishedAt = OffsetDateTime.now(ZoneId.of("Asia/Seoul"))
                )

                producer.send(news)
                processedIds.add(item.rceptNo)
                sent++
            }

            if (sent > 0) log.info("[DART] $sent 건 Kafka 전송 완료")

        } catch (e: Exception) {
            log.error("[DART] 수집 실패: ${e.message}", e)
        }
    }

    private fun buildDartContent(item: DartDisclosure): String {
        return buildString {
            append("공시유형: ${item.reportNm ?: "N/A"}\n")
            append("제출인: ${item.flrNm ?: "N/A"}\n")
            append("접수번호: ${item.rceptNo}\n")
            append("공시일: ${item.rceptDt ?: "N/A"}\n")
            item.rmk?.let { append("비고: $it\n") }
        }
    }
}

// DART OpenAPI 응답 모델
data class DartListResponse(
    val status: String?,
    val message: String?,
    val page_no: Int?,
    val page_count: Int?,
    val total_count: Int?,
    val total_page: Int?,
    val list: List<DartDisclosure>?
)

data class DartDisclosure(
    val corp_code: String?,
    val corp_name: String?,
    val stock_code: String?,
    val corp_cls: String?,
    val report_nm: String?,
    val rcept_no: String = "",
    val flr_nm: String?,
    val rcept_dt: String?,
    val rmk: String?
) {
    // snake_case JSON → camelCase 접근용
    val corpName get() = corp_name
    val stockCode get() = stock_code
    val reportNm get() = report_nm
    val rceptNo get() = rcept_no
    val flrNm get() = flr_nm
    val rceptDt get() = rcept_dt
}
