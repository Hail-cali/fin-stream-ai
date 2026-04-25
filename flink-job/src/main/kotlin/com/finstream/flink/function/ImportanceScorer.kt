package com.finstream.flink.function

import com.finstream.flink.model.EnrichedNews
import com.finstream.flink.model.RawNews
import org.apache.flink.api.common.functions.MapFunction

/**
 * 키워드 기반 중요도 스코어링 (0.0 ~ 1.0).
 * LLM 호출 없이 빠르게 필터링.
 */
class ImportanceScorer : MapFunction<RawNews, EnrichedNews> {

    // 중요도 키워드 → 가중치
    private val keywordWeights = mapOf(
        // 높은 중요도 (0.3)
        "실적" to 0.3, "매출" to 0.3, "영업이익" to 0.3, "순이익" to 0.3,
        "분기보고서" to 0.3, "반기보고서" to 0.3, "사업보고서" to 0.3,
        "배당" to 0.3, "자사주" to 0.3, "유상증자" to 0.3,
        "인수" to 0.3, "합병" to 0.3, "M&A" to 0.3,
        "상장폐지" to 0.3, "관리종목" to 0.3,

        // 중간 중요도 (0.2)
        "공시" to 0.2, "주가" to 0.2, "시가총액" to 0.2,
        "투자" to 0.2, "수주" to 0.2, "계약" to 0.2,
        "신사업" to 0.2, "특허" to 0.2, "기술" to 0.2,
        "HBM" to 0.2, "반도체" to 0.2, "AI" to 0.2,
        "수출" to 0.2, "규제" to 0.2,

        // 낮은 중요도 (0.1)
        "임원" to 0.1, "대표이사" to 0.1, "이사회" to 0.1,
        "소송" to 0.1, "벌금" to 0.1, "제재" to 0.1,
        "주주총회" to 0.1, "정관" to 0.1
    )

    override fun map(news: RawNews): EnrichedNews {
        val combined = "${news.title} ${news.content}"

        var score = 0.0
        for ((keyword, weight) in keywordWeights) {
            if (keyword in combined) {
                score += weight
            }
        }

        // 0.0 ~ 1.0 범위로 클램핑
        val normalizedScore = score.coerceIn(0.0, 1.0)

        return EnrichedNews(
            newsId = news.newsId,
            stockCode = news.stockCode,
            stockName = news.stockName,
            title = news.title,
            content = news.content,
            importanceScore = normalizedScore
        )
    }
}
