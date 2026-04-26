package com.ainsoft.ai.dto

import jakarta.validation.constraints.NotBlank

data class ChatRequest(
    @field:NotBlank(message = "message must not be blank")
    val message: String,
    val voice: String? = null,
    val conversationId: String? = null
)
