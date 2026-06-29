package com.finstream.api.controller

import com.finstream.api.model.SearchRequest
import com.finstream.api.model.SearchResult
import com.finstream.api.service.SearchService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/search")
class SearchController(
    private val searchService: SearchService
) {

    @PostMapping
    fun search(@RequestBody request: SearchRequest): ResponseEntity<List<SearchResult>> {
        val results = searchService.search(
            query = request.query,
            stockCode = request.stockCode,
            topK = request.topK.coerceIn(1, 20)
        )
        return ResponseEntity.ok(results)
    }
}
