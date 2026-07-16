package com.ainsoft.ai.controller

import com.ainsoft.ai.dto.ChatMemorySnapshot
import com.ainsoft.ai.dto.ChatResult
import com.ainsoft.ai.dto.DocumentSummary
import com.ainsoft.ai.dto.RagQueryRequest
import com.ainsoft.ai.dto.RagResponse
import com.ainsoft.ai.dto.ChatRequest
import com.ainsoft.ai.dto.TextToSpeechRequest
import com.ainsoft.ai.service.ChatService
import com.ainsoft.ai.service.RagService
import com.ainsoft.ai.service.TextToSpeechService
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import reactor.core.publisher.Flux

class ControllerSmokeTest {

    private val validator = LocalValidatorFactoryBean().apply { afterPropertiesSet() }

    @Test
    fun `chat endpoint returns service response`() {
        val chatService = mock(ChatService::class.java)
        doReturn(ChatResult("hello", "neutral"))
            .`when`(chatService)
            .ask(any(ChatRequest::class.java) ?: ChatRequest("ignored"))

        val mockMvc = MockMvcBuilders.standaloneSetup(ChatController(chatService))
            .setControllerAdvice(GlobalExceptionHandler())
            .setValidator(validator)
            .build()

        mockMvc.post("/api/chat") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"message":"Hi"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.message") { value("hello") }
            jsonPath("$.voice") { value("neutral") }
        }
    }

    @Test
    fun `rag query validation rejects invalid topK`() {
        val ragService = mock(RagService::class.java)
        val mockMvc = MockMvcBuilders.standaloneSetup(RagController(ragService))
            .setControllerAdvice(GlobalExceptionHandler())
            .setValidator(validator)
            .build()

        mockMvc.post("/api/rag/query") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"question":"What?","topK":0}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value("Request validation failed") }
        }
    }

    @Test
    fun `rag query endpoint returns answer and context`() {
        val ragService = mock(RagService::class.java)
        doReturn(RagResponse("answer", listOf(DocumentSummary("doc-1", "context"))))
            .`when`(ragService)
            .query(any(RagQueryRequest::class.java) ?: RagQueryRequest("ignored"))

        val mockMvc = MockMvcBuilders.standaloneSetup(RagController(ragService))
            .setControllerAdvice(GlobalExceptionHandler())
            .setValidator(validator)
            .build()

        mockMvc.post("/api/rag/query") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"question":"What?"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.answer") { value("answer") }
            jsonPath("$.context[0].id") { value("doc-1") }
        }
    }

    @Test
    fun `tts endpoint returns audio bytes`() {
        val textToSpeechService = mock(TextToSpeechService::class.java)
        doReturn("RIFF".toByteArray() to MediaType.parseMediaType("audio/wav"))
            .`when`(textToSpeechService)
            .synthesize(any(TextToSpeechRequest::class.java) ?: TextToSpeechRequest("ignored"))

        val mockMvc = MockMvcBuilders.standaloneSetup(TextToSpeechController(textToSpeechService))
            .setControllerAdvice(GlobalExceptionHandler())
            .setValidator(validator)
            .build()

        mockMvc.post("/api/tts") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"text":"Hello"}"""
        }.andExpect {
            status { isOk() }
            header { string("Content-Type", "audio/wav") }
            content { bytes("RIFF".toByteArray()) }
        }
    }

    @Test
    fun `rag delete returns 204 when document exists`() {
        val ragService = mock(RagService::class.java)
        doReturn(true).`when`(ragService).deleteDocument(anyString())

        val mockMvc = MockMvcBuilders.standaloneSetup(RagController(ragService))
            .setControllerAdvice(GlobalExceptionHandler())
            .build()

        mockMvc.delete("/api/rag/documents/doc-1")
            .andExpect { status { isNoContent() } }
    }

    @Test
    fun `rag delete returns 404 when document not found`() {
        val ragService = mock(RagService::class.java)
        doReturn(false).`when`(ragService).deleteDocument(anyString())

        val mockMvc = MockMvcBuilders.standaloneSetup(RagController(ragService))
            .setControllerAdvice(GlobalExceptionHandler())
            .build()

        mockMvc.delete("/api/rag/documents/missing")
            .andExpect { status { isNotFound() } }
    }

    @Test
    fun `clear memory returns 204`() {
        val chatService = mock(ChatService::class.java)
        val mockMvc = MockMvcBuilders.standaloneSetup(ChatController(chatService))
            .setControllerAdvice(GlobalExceptionHandler())
            .build()

        mockMvc.delete("/api/chat/memory/session-1")
            .andExpect { status { isNoContent() } }
    }

    @Test
    fun `stream endpoint returns SSE event stream`() {
        val chatService = mock(ChatService::class.java)
        doReturn(Flux.just("Hello", " world"))
            .`when`(chatService)
            .streamAsk(any(ChatRequest::class.java) ?: ChatRequest("ignored"))

        val mockMvc = MockMvcBuilders.standaloneSetup(ChatController(chatService))
            .setControllerAdvice(GlobalExceptionHandler())
            .setValidator(validator)
            .build()

        mockMvc.post("/api/chat/stream") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"message":"Hi"}"""
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `memory get returns snapshot`() {
        val chatService = mock(ChatService::class.java)
        doReturn(ChatMemorySnapshot("s1", listOf("hello")))
            .`when`(chatService)
            .history(anyString())

        val mockMvc = MockMvcBuilders.standaloneSetup(ChatController(chatService))
            .setControllerAdvice(GlobalExceptionHandler())
            .build()

        mockMvc.get("/api/chat/memory/s1")
            .andExpect {
                status { isOk() }
                jsonPath("$.conversationId") { value("s1") }
            }
    }
}
