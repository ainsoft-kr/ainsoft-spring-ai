package com.ainsoft.ai.controller

import com.ainsoft.ai.dto.DocumentPayload
import com.ainsoft.ai.dto.DocumentSummary
import com.ainsoft.ai.dto.RagQueryRequest
import com.ainsoft.ai.dto.RagResponse
import com.ainsoft.ai.service.RagService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/rag")
class RagController(
    private val ragService: RagService
) {

    @PostMapping("/documents")
    fun addDocuments(@Valid @RequestBody payload: List<DocumentPayload>): ResponseEntity<List<DocumentSummary>> {
        return ResponseEntity.ok(ragService.ingest(payload))
    }

    @GetMapping("/documents")
    fun listDocuments(): ResponseEntity<List<DocumentSummary>> {
        return ResponseEntity.ok(ragService.list())
    }

    @PostMapping("/query")
    fun query(@Valid @RequestBody request: RagQueryRequest): ResponseEntity<RagResponse> {
        return ResponseEntity.ok(ragService.query(request))
    }
}
