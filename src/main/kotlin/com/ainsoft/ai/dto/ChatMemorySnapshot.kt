package com.ainsoft.ai.dto

data class ChatMemorySnapshot(
    val conversationId: String,
    val messages: List<String>
)
