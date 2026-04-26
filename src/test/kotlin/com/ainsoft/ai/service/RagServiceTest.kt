package com.ainsoft.ai.service

import com.ainsoft.ai.config.RagProperties
import com.ainsoft.ai.dto.DocumentPayload
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.mock
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.document.Document
import org.springframework.ai.embedding.Embedding
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.embedding.EmbeddingRequest
import org.springframework.ai.embedding.EmbeddingResponse
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.SimpleVectorStore
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.ai.vectorstore.filter.Filter
import java.nio.file.Path

class RagServiceTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `ingest persists document index and restores it on startup`() {
        val properties = RagProperties().apply {
            storePath = tempDir.resolve("vector-store.json")
            documentIndexPath = tempDir.resolve("documents.json")
        }
        val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
        val vectorStore = RecordingVectorStore()

        val service = RagService(mock(ChatClient::class.java), vectorStore, properties, objectMapper)
        val summaries = service.ingest(
            listOf(
                DocumentPayload(id = "b", text = "  second  ", source = ""),
                DocumentPayload(id = "a", text = "first", source = "docs")
            )
        )

        assertThat(summaries.map { it.id }).containsExactly("b", "a")
        assertThat(vectorStore.added.map { it.text }).containsExactly("second", "first")
        assertThat(properties.documentIndexPath).exists()

        val restored = RagService(mock(ChatClient::class.java), RecordingVectorStore(), properties, objectMapper)
        restored.loadDocumentIndex()

        assertThat(restored.list().map { it.id }).containsExactly("a", "b")
    }

    @Test
    fun `simple vector store persists and restores searchable vectors`() {
        val storePath = tempDir.resolve("vector-store.json")
        val embeddingModel = DeterministicEmbeddingModel()
        val originalStore = SimpleVectorStore.builder(embeddingModel).build()

        originalStore.add(
            listOf(
                Document.builder().id("spring").text("Spring AI supports vector search").build(),
                Document.builder().id("piper").text("Piper generates local speech").build()
            )
        )
        originalStore.save(storePath.toFile())

        val restoredStore = SimpleVectorStore.builder(embeddingModel).build()
        restoredStore.load(storePath.toFile())

        val results = restoredStore.similaritySearch(
            SearchRequest.builder()
                .query("vector search")
                .topK(1)
                .build()
        )

        assertThat(results).hasSize(1)
        assertThat(results.first().id).isEqualTo("spring")
    }

    private class RecordingVectorStore : VectorStore {
        val added = mutableListOf<Document>()

        override fun add(documents: List<Document>) {
            added += documents
        }

        override fun delete(idList: List<String>) = Unit

        override fun delete(filterExpression: Filter.Expression) = Unit

        override fun similaritySearch(request: SearchRequest): List<Document> = emptyList()
    }

    private class DeterministicEmbeddingModel : EmbeddingModel {

        override fun call(request: EmbeddingRequest): EmbeddingResponse {
            val embeddings = request.instructions.mapIndexed { index, text ->
                Embedding(vectorFor(text), index)
            }
            return EmbeddingResponse(embeddings)
        }

        override fun embed(document: Document): FloatArray {
            return vectorFor(document.text ?: "")
        }

        override fun dimensions(): Int = 3

        private fun vectorFor(text: String): FloatArray {
            val lower = text.lowercase()
            return floatArrayOf(
                if ("spring" in lower || "vector" in lower || "search" in lower) 1.0f else 0.0f,
                if ("piper" in lower || "speech" in lower) 1.0f else 0.0f,
                0.1f
            )
        }
    }
}
