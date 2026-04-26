package com.ainsoft.ai.service

import com.ainsoft.ai.config.ChatApiProperties
import com.ainsoft.ai.dto.ChatMemorySnapshot
import com.ainsoft.ai.dto.ChatRequest
import com.ainsoft.ai.dto.ChatResult
import com.ainsoft.ai.dto.MultimodalRequest
import com.ainsoft.ai.dto.OcrRequest
import com.ainsoft.ai.dto.OcrResult
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import org.springframework.util.MimeType
import org.springframework.util.MimeTypeUtils
import org.springframework.util.StringUtils
import org.springframework.web.multipart.MultipartFile

@Service
class ChatService(
    private val chatClient: ChatClient,
    private val chatMemory: ChatMemory,
    private val properties: ChatApiProperties
) {

    fun ask(request: ChatRequest): ChatResult {
        val conversationId = normalizedConversationId(request.conversationId)
        val voice = resolveVoice(request.voice)
        val prompt = newPrompt(voice, conversationId)

        val response = prompt.user(request.message).call()
        val message = response.content() ?: ""
        return ChatResult(message, voice)
    }

    fun askWithMedia(request: MultimodalRequest, media: MultipartFile?): ChatResult {
        val conversationId = normalizedConversationId(request.conversationId)
        val voice = resolveVoice(request.voice)
        val prompt = newPrompt(voice, conversationId)

        val response = prompt.user { user ->
            user.text(request.message)
            media?.let { user.media(resolveMimeType(it), toResource(it)) }
        }.call()

        val message = response.content() ?: ""
        return ChatResult(message, voice)
    }

    fun extractText(media: MultipartFile, request: OcrRequest): OcrResult {
        val conversationId = normalizedConversationId(request.conversationId)
        val voice = resolveVoice(request.voice)
        val prompt = newPrompt(voice, conversationId)

        val response = prompt.user { user ->
            user.text(request.instructions)
            user.media(resolveMimeType(media), toResource(media))
        }.call()

        val extracted = response.content() ?: ""
        return OcrResult(extracted, voice)
    }

    fun history(conversationId: String): ChatMemorySnapshot {
        val id = normalizedConversationId(conversationId)
        val messages = chatMemory.get(id).mapNotNull { it.text }
        return ChatMemorySnapshot(id, messages)
    }

    private fun normalizedConversationId(conversationId: String?): String {
        return conversationId?.takeIf { it.isNotBlank() } ?: ChatMemory.DEFAULT_CONVERSATION_ID
    }

    private fun resolveVoice(voice: String?): String {
        return voice?.takeIf { it.isNotBlank() } ?: properties.defaultVoice
    }

    private fun newPrompt(voice: String, conversationId: String) = chatClient.prompt().apply {
        advisors(
            MessageChatMemoryAdvisor.builder(chatMemory)
                .conversationId(conversationId)
                .build()
        )
        if (StringUtils.hasText(properties.systemPrompt)) {
            system { spec -> spec.text(properties.systemPrompt).param("voice", voice) }
        }
    }

    private fun resolveMimeType(file: MultipartFile): MimeType {
        return file.contentType?.let { MimeTypeUtils.parseMimeType(it) } ?: MimeTypeUtils.APPLICATION_OCTET_STREAM
    }

    private fun toResource(file: MultipartFile): Resource = object : ByteArrayResource(file.bytes) {
        override fun getFilename(): String? = file.originalFilename ?: file.name
    }
}
