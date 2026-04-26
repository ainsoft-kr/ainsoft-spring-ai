package com.ainsoft.ai.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class RagQueryRequest(
    @field:NotBlank(message = "question must not be blank")
    val question: String,
    @field:Min(value = 1, message = "topK must be at least 1")
    @field:Max(value = 50, message = "topK must be at most 50")
    val topK: Int? = null,
    @field:DecimalMin(value = "0.0", message = "similarityThreshold must be at least 0.0")
    @field:DecimalMax(value = "1.0", message = "similarityThreshold must be at most 1.0")
    val similarityThreshold: Double? = null,
    val filterExpression: String? = null
)
