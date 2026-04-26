package com.ainsoft.ai.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.vectorstore.SimpleVectorStore
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Qualifier

@Configuration
class RagConfiguration(
    private val aiProviderProperties: AiProviderProperties,
    private val ragProperties: RagProperties
) {

    @Bean
    fun vectorStore(
        @Qualifier("ollamaEmbeddingModel") ollamaEmbeddingModelProvider: ObjectProvider<EmbeddingModel>,
        @Qualifier("openAiEmbeddingModel") openAiEmbeddingModelProvider: ObjectProvider<EmbeddingModel>,
        @Qualifier("ociEmbeddingModel") ociEmbeddingModelProvider: ObjectProvider<EmbeddingModel>
    ): VectorStore {
        val provider = aiProviderProperties.provider.lowercase()
        val embeddingModel = when (provider) {
            "ollama" -> ollamaEmbeddingModelProvider.getIfAvailable()
            "openai" -> openAiEmbeddingModelProvider.getIfAvailable()
            "oci" -> ociEmbeddingModelProvider.getIfAvailable()
            else -> error("Unsupported AI provider '${aiProviderProperties.provider}'")
        } ?: error("No embedding model available for provider '${aiProviderProperties.provider}'")

        val vectorStore = SimpleVectorStore.builder(embeddingModel).build()
        val storeFile = ragProperties.storePath.toFile()
        if (storeFile.isFile) {
            vectorStore.load(storeFile)
        }
        return vectorStore
    }

    @Bean
    fun chatClient(
        @Qualifier("ollamaChatModel") ollamaChatModelProvider: ObjectProvider<ChatModel>,
        @Qualifier("openAiChatModel") openAiChatModelProvider: ObjectProvider<ChatModel>,
        @Qualifier("ociChatModel") ociChatModelProvider: ObjectProvider<ChatModel>
    ): ChatClient {
        val provider = aiProviderProperties.provider.lowercase()
        val chatModel = when (provider) {
            "ollama" -> ollamaChatModelProvider.getIfAvailable()
            "openai" -> openAiChatModelProvider.getIfAvailable()
            "oci" -> ociChatModelProvider.getIfAvailable()
            else -> error("Unsupported AI provider '${aiProviderProperties.provider}'")
        } ?: error("No chat model available for provider '${aiProviderProperties.provider}'")

        return ChatClient.builder(chatModel).build()
    }
}
