package com.ainsoft.ai.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin


data class TextToSpeechRequest(
    @field:NotBlank
    val text: String,
    val voice: String? = null,
    val format: String? = null,
    @field:DecimalMin(value = "0.5", message = "speed must be at least 0.5")
    @field:DecimalMax(value = "2.0", message = "speed must be at most 2.0")
    val speed: Double? = null
)
