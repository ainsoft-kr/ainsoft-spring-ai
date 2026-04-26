package com.ainsoft.ai.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tts")
class TtsProperties {
    var modelPath: java.nio.file.Path = java.nio.file.Path.of("./data/model")
    var defaultVoice: String = "default"
    var defaultFormat: String = "wav"
    var defaultSpeed: Double = 1.0
    var voices: Map<String, VoiceConfig> = mapOf(
        "default" to VoiceConfig(
            model = "en_US-lessac-medium.onnx",
            config = "en_US-lessac-medium.onnx.json",
            sampleRate = 22050
        )
    )

    data class VoiceConfig(
        val model: String,
        val config: String,
        val sampleRate: Int
    )
}
