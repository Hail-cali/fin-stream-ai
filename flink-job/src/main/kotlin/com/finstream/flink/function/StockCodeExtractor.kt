package com.finstream.flink.function

import com.finstream.flink.model.RawNews
import org.apache.flink.api.common.functions.MapFunction

/**
 * 종목코드 검증 및 보정.
 * collector에서 이미 stock_code를 넣어주지만,
 * RSS 소스의 경우 추가 검증/보정이 필요할 수 있다.
 */
class StockCodeExtractor : MapFunction<RawNews, RawNews> {

    // 종목명 → (종목코드, 정규 종목명)
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
        "포스코홀딩스" to Pair("003670", "포스코홀딩스"),
        "KB금융" to Pair("105560", "KB금융"),
        "신한지주" to Pair("055550", "신한지주"),
        "LG에너지솔루션" to Pair("373220", "LG에너지솔루션"),
        "셀트리온" to Pair("068270", "셀트리온"),
        "기아" to Pair("000270", "기아"),
        "현대모비스" to Pair("012330", "현대모비스"),
        "LG전자" to Pair("066570", "LG전자"),
        "크래프톤" to Pair("259960", "크래프톤")
    )

    // 유효한 종목코드 set (6자리 숫자)
    private val validCodes = stockDictionary.values.map { it.first }.toSet()

    override fun map(news: RawNews): RawNews {
        // 이미 유효한 종목코드가 있으면 그대로 반환
        if (news.stockCode.length == 6 && news.stockCode in validCodes) {
            return news
        }

        // 종목코드가 비어있거나 유효하지 않으면 제목/본문에서 추출
        val combined = "${news.title} ${news.content}"
        for ((keyword, pair) in stockDictionary.entries.sortedByDescending { it.key.length }) {
            if (keyword in combined) {
                return news.copy(
                    stockCode = pair.first,
                    stockName = pair.second
                )
            }
        }

        // 매칭 실패 → 원본 그대로 (downstream filter에서 제거)
        return news
    }
}
