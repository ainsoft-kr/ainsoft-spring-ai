package com.ainsoft.ai.dto

import jakarta.validation.constraints.NotBlank

data class DocumentPayload(
    val id: String? = null,
    @field:NotBlank(message = "text must not be blank")
    val text: String,
    val source: String? = null,
    val metadata: Map<String, String>? = null
)
