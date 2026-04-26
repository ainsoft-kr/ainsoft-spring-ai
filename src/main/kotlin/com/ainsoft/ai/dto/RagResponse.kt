package com.ainsoft.ai.dto

data class RagResponse(
    val answer: String,
    val context: List<DocumentSummary>
)
