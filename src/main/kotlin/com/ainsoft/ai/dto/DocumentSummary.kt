package com.ainsoft.ai.dto

data class DocumentSummary(
    val id: String,
    val content: String,
    val metadata: Map<String, Any?> = emptyMap()
)
