package com.finstream.api.controller

import com.finstream.api.model.AnalysisResult
import com.finstream.api.service.AnalysisService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/stocks")
class AnalysisController(
    private val analysisService: AnalysisService
) {

    @GetMapping("/{code}/analysis")
    fun getAnalysis(
        @PathVariable code: String,
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseEntity<List<AnalysisResult>> {
        val results = analysisService.getByStockCode(code, limit.coerceIn(1, 100))
        return ResponseEntity.ok(results)
    }

    @GetMapping("/{code}/analysis/latest")
    fun getLatest(@PathVariable code: String): ResponseEntity<AnalysisResult> {
        val result = analysisService.getLatest(code)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(result)
    }

    @GetMapping("/recent")
    fun getRecent(
        @RequestParam(defaultValue = "50") limit: Int
    ): ResponseEntity<List<AnalysisResult>> {
        return ResponseEntity.ok(analysisService.getRecentAll(limit.coerceIn(1, 100)))
    }
}
