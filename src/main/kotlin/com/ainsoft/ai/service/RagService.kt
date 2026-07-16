package com.ainsoft.ai.service

import com.ainsoft.ai.config.RagProperties
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.ainsoft.ai.dto.DocumentPayload
import com.ainsoft.ai.dto.DocumentSummary
import com.ainsoft.ai.dto.RagQueryRequest
import com.ainsoft.ai.dto.RagResponse
import jakarta.annotation.PostConstruct
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.ChatClientResponse
import org.springframework.ai.chat.client.advisor.api.Advisor
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.SimpleVectorStore
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap


@Service
class RagService(
    private val chatClient: ChatClient,
    private val vectorStore: VectorStore,
    private val properties: RagProperties,
    private val objectMapper: ObjectMapper
) {

    private val documentIndex = ConcurrentHashMap<String, DocumentSummary>()

    @PostConstruct
    fun loadDocumentIndex() {
        val path = properties.documentIndexPath
        if (!Files.isRegularFile(path)) {
            return
        }

        val summaries = objectMapper.readValue(
            path.toFile(),
            object : TypeReference<List<DocumentSummary>>() {}
        )
        summaries.forEach { summary -> documentIndex[summary.id] = summary }
    }

    fun ingest(payloads: List<DocumentPayload>): List<DocumentSummary> {
        if (payloads.isEmpty()) {
            return emptyList()
        }

        val docs = payloads.map { payload ->
            val id = payload.id?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
            val source = payload.source?.takeIf { it.isNotBlank() } ?: "user"
            val builder = Document.builder()
                .id(id)
                .text(payload.text.trim())

            payload.metadata?.forEach { (key, value) -> builder.metadata(key, value) }

            builder.metadata("source", source).build()
        }

        vectorStore.add(docs)

        return docs.map { doc ->
            val summary = DocumentSummary(
                id = requireDocumentId(doc),
                content = requireDocumentText(doc),
                metadata = doc.metadata
            )
            documentIndex[summary.id] = summary
            summary
        }.also { persistIfEnabled() }
    }

    fun list(): List<DocumentSummary> = documentIndex.values.sortedBy { it.id }

    fun deleteDocument(id: String): Boolean {
        val existed = documentIndex.remove(id) != null
        if (existed) {
            vectorStore.delete(listOf(id))
            persistIfEnabled()
        }
        return existed
    }

    fun query(request: RagQueryRequest): RagResponse {
        val question = request.question.trim()
        val builder = SearchRequest.builder()
            .query(question)

        request.topK?.let { builder.topK(it) }
        request.similarityThreshold?.let { builder.similarityThreshold(it) }
        request.filterExpression?.let { builder.filterExpression(it) }

        val searchRequest: SearchRequest = builder.build()

        val advisor: Advisor = QuestionAnswerAdvisor.builder(vectorStore)
            .searchRequest(searchRequest)
            .build()

        val responseSpec = chatClient.prompt()
            .advisors(advisor)
            .user(question)
            .call()

        val answer = responseSpec.content() ?: ""
        val clientResponse: ChatClientResponse = responseSpec.chatClientResponse()
        val retrieved = (clientResponse.context()[QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS] as? List<*>)
            ?.filterIsInstance<Document>()
            ?: emptyList()
        val context = retrieved.map { doc ->
            DocumentSummary(requireDocumentId(doc), requireDocumentText(doc), doc.metadata)
        }

        return RagResponse(answer, context)
    }

    private fun requireDocumentId(document: Document): String {
        return requireNotNull(document.id) { "Vector store returned a document without an id" }
    }

    private fun requireDocumentText(document: Document): String {
        return requireNotNull(document.text) { "Vector store returned a document without text" }
    }

    private fun persistIfEnabled() {
        if (!properties.autoSave) {
            return
        }

        properties.storePath.parent?.let { Files.createDirectories(it) }
        (vectorStore as? SimpleVectorStore)?.save(properties.storePath.toFile())
        properties.documentIndexPath.parent?.let { Files.createDirectories(it) }
        objectMapper.writerWithDefaultPrettyPrinter()
            .writeValue(properties.documentIndexPath.toFile(), documentIndex.values.sortedBy { it.id })
    }
}
