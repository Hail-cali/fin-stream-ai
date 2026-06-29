package com.finstream.api.service

import com.finstream.api.model.AnalysisResult
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque

@Service
class AnalysisService {

    companion object {
        private const val MAX_PER_STOCK = 100
        private const val MAX_GLOBAL = 500
    }

    // stock_code → 최근 분석 결과 (최신순)
    private val store = ConcurrentHashMap<String, ConcurrentLinkedDeque<AnalysisResult>>()

    // 전체 최신 순 (SSE 피드용)
    private val globalFeed = ConcurrentLinkedDeque<AnalysisResult>()

    fun save(result: AnalysisResult) {
        // 종목별 저장
        val deque = store.computeIfAbsent(result.stockCode) { ConcurrentLinkedDeque() }
        deque.addFirst(result)
        while (deque.size > MAX_PER_STOCK) deque.removeLast()

        // 글로벌 피드
        globalFeed.addFirst(result)
        while (globalFeed.size > MAX_GLOBAL) globalFeed.removeLast()
    }

    fun getByStockCode(stockCode: String, limit: Int = 20): List<AnalysisResult> {
        return store[stockCode]?.take(limit) ?: emptyList()
    }

    fun getLatest(stockCode: String): AnalysisResult? {
        return store[stockCode]?.peekFirst()
    }

    fun getRecentAll(limit: Int = 50): List<AnalysisResult> {
        return globalFeed.take(limit)
    }
}
