package com.finstream.collector.source

import com.finstream.collector.model.RawNews
import com.finstream.collector.producer.KafkaNewsProducer
import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.net.URI
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.*

@Component
class RssCollector(
    private val producer: KafkaNewsProducer,
    @Value("\${collector.rss.enabled:true}") private val enabled: Boolean,
    @Value("\${collector.rss.feeds:}") private val feedUrls: List<String>
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // 종목명 → 종목코드 매핑 (사전 기반 NER)
    private val stockDictionary = mapOf(
        "삼성전자" to Pair("005930", "삼성전자"),
        "삼성" to Pair("005930", "삼성전자"),
        "SK하이닉스" to Pair("000660", "SK하이닉스"),
        "하이닉스" to Pair("000660", "SK하이닉스"),
        "네이버" to Pair("035420", "NAVER"),
        "NAVER" to Pair("035420", "NAVER"),
        "카카오" to Pair("035720", "카카오"),
        "현대차" to Pair("005380", "현대자동차"),
        "현대자동차" to Pair("005380", "현대자동차"),
        "삼성SDI" to Pair("006400", "삼성SDI"),
        "LG화학" to Pair("051910", "LG화학"),
        "포스코" to Pair("003670", "포스코홀딩스"),
        "KB금융" to Pair("105560", "KB금융"),
        "신한지주" to Pair("055550", "신한지주")
    )

    // 이미 처리한 뉴스 링크 추적
    private val processedLinks = Collections.synchronizedSet(mutableSetOf<String>())

    @Scheduled(fixedDelay = 600_000, initialDelay = 10_000) // 10분 주기, 시작 10초 후
    fun collect() {
        val validFeeds = feedUrls.filter { it.isNotBlank() }
        if (!enabled || validFeeds.isEmpty()) {
            log.debug("[RSS] 비활성화 또는 피드 URL 미설정")
            return
        }

        var totalSent = 0

        for (feedUrl in validFeeds) {
            try {
                val input = SyndFeedInput()
                val feed = input.build(XmlReader(URI(feedUrl).toURL()))

                for (entry in feed.entries) {
                    val link = entry.link ?: continue
                    if (link in processedLinks) continue

                    val title = entry.title ?: continue
                    val content = entry.description?.value
                        ?: entry.contents?.firstOrNull()?.value
                        ?: title

                    // 종목명 추출 (제목 + 본문에서 사전 매칭)
                    val matchedStock = extractStockCode(title, content)
                    if (matchedStock == null) {
                        processedLinks.add(link)
                        continue // 종목 매칭 실패 → 스킵
                    }

                    val publishedAt = entry.publishedDate?.let {
                        OffsetDateTime.ofInstant(it.toInstant(), ZoneId.of("Asia/Seoul"))
                    } ?: OffsetDateTime.now(ZoneId.of("Asia/Seoul"))

                    val news = RawNews(
                        newsId = "rss-${link.hashCode().toUInt()}",
                        stockCode = matchedStock.first,
                        stockName = matchedStock.second,
                        title = title,
                        content = cleanHtml(content),
                        source = "RSS",
                        publishedAt = publishedAt
                    )

                    producer.send(news)
                    processedLinks.add(link)
                    totalSent++
                }
            } catch (e: Exception) {
                log.error("[RSS] 피드 수집 실패: $feedUrl - ${e.message}")
            }
        }

        if (totalSent > 0) log.info("[RSS] $totalSent 건 Kafka 전송 완료")

        // 메모리 관리: 24시간 지난 링크 정리 (간단히 1만 건 초과 시 클리어)
        if (processedLinks.size > 10_000) {
            processedLinks.clear()
            log.info("[RSS] processedLinks 캐시 초기화 (10,000건 초과)")
        }
    }

    private fun extractStockCode(title: String, content: String): Pair<String, String>? {
        val combined = "$title $content"
        // 긴 키워드부터 매칭 (삼성SDI → 삼성 순서 방지)
        for ((keyword, pair) in stockDictionary.entries.sortedByDescending { it.key.length }) {
            if (keyword in combined) return pair
        }
        return null
    }

    private fun cleanHtml(html: String): String {
        return html.replace(Regex("<[^>]*>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .trim()
    }
}
