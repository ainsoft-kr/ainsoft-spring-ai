package com.ainsoft.ai.dto

import jakarta.validation.constraints.NotBlank

data class OcrRequest(
    @field:NotBlank(message = "instructions must not be blank")
    val instructions: String,
    val voice: String? = null,
    val conversationId: String? = null
)
