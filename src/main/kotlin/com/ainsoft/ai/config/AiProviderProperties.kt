package com.ainsoft.ai.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "ai")
class AiProviderProperties {
    var provider: String = "ollama"
}
