package com.ainsoft.ai.dto

import jakarta.validation.Validation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RequestValidationTest {

    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `rejects invalid rag query options`() {
        val violations = validator.validate(
            RagQueryRequest(
                question = "What is stored?",
                topK = 0,
                similarityThreshold = 1.5
            )
        )

        val fields = violations.map { it.propertyPath.toString() }.toSet()
        assertThat(fields).contains("topK", "similarityThreshold")
    }

    @Test
    fun `rejects invalid tts speed`() {
        val violations = validator.validate(
            TextToSpeechRequest(
                text = "hello",
                speed = 2.5
            )
        )

        val fields = violations.map { it.propertyPath.toString() }.toSet()
        assertThat(fields).contains("speed")
    }
}
