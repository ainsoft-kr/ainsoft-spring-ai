package com.ainsoft.ai.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "chat.api")
class ChatApiProperties {

    companion object {
        private const val DEFAULT_SYSTEM_PROMPT = "You are a helpful assistant that replies in the {voice} voice."
        private const val DEFAULT_VOICE = "neutral"
    }

    var systemPrompt: String = DEFAULT_SYSTEM_PROMPT
        set(value) {
            field = if (value.isBlank()) DEFAULT_SYSTEM_PROMPT else value
        }

    var defaultVoice: String = DEFAULT_VOICE
        set(value) {
            field = if (value.isBlank()) DEFAULT_VOICE else value
        }
}
