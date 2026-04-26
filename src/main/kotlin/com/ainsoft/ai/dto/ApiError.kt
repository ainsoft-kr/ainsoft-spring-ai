package com.ainsoft.ai.dto

data class ApiError(
    val message: String,
    val details: List<String> = emptyList()
)
