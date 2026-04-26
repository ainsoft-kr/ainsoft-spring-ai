package com.ainsoft.ai.service

import com.ainsoft.ai.config.TtsProperties
import com.ainsoft.ai.dto.TextToSpeechRequest
import jakarta.annotation.PreDestroy
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.AudioInputStream
import kotlin.math.roundToInt
import io.github.givimad.piperjni.PiperJNI
import io.github.givimad.piperjni.PiperVoice

@Service
class TextToSpeechService(
    private val properties: TtsProperties
) {

    private val piper: PiperJNI = PiperJNI().apply { initialize() }
    private val voices = ConcurrentHashMap<String, PiperVoice>()

    @PreDestroy
    fun shutdown() {
        voices.values.forEach { it.close() }
        voices.clear()
        piper.close()
    }

    fun synthesize(request: TextToSpeechRequest): Pair<ByteArray, MediaType> {
        val voice = request.voice?.takeIf { it.isNotBlank() } ?: properties.defaultVoice
        val format = normalizeFormat(request.format)
        val speed = request.speed ?: properties.defaultSpeed

        val voiceSpec = resolveVoiceSpec(voice)
        val samples = adjustSpeed(generateSamples(request.text, voiceSpec), speed)
        return when (format) {
            "wav" -> encodeWav(samples, voiceSpec.sampleRate) to MediaType.parseMediaType("audio/wav")
            "pcm" -> encodePcm(samples) to MediaType.parseMediaType("audio/pcm")
            else -> error("Unexpected TTS format '$format'")
        }
    }

    private fun normalizeFormat(format: String?): String {
        val normalized = format?.takeIf { it.isNotBlank() } ?: properties.defaultFormat
        return normalized.lowercase().also {
            require(it == "wav" || it == "pcm") { "Unsupported TTS format '$normalized'. Supported formats are wav and pcm." }
        }
    }

    private fun resolveVoiceSpec(voice: String): VoiceSpec {
        val entry = properties.voices[voice] ?: properties.voices[properties.defaultVoice]
        requireNotNull(entry) { "No voice config for '$voice'" }
        val spec = VoiceSpec(
            modelPath = properties.modelPath.resolve(entry.model),
            configPath = properties.modelPath.resolve(entry.config),
            sampleRate = entry.sampleRate
        )
        require(Files.isRegularFile(spec.modelPath)) { "Piper model file does not exist: ${spec.modelPath}" }
        require(Files.isRegularFile(spec.configPath)) { "Piper config file does not exist: ${spec.configPath}" }
        return spec
    }

    private fun generateSamples(text: String, voiceSpec: VoiceSpec): ShortArray {
        val voice = voices.computeIfAbsent(voiceSpec.cacheKey) {
            piper.loadVoice(voiceSpec.modelPath, voiceSpec.configPath)
        }
        synchronized(voice) {
            return piper.textToAudio(voice, text)
        }
    }

    private fun adjustSpeed(samples: ShortArray, speed: Double): ShortArray {
        require(speed in 0.5..2.0) { "speed must be between 0.5 and 2.0" }
        if (speed == 1.0 || samples.isEmpty()) {
            return samples
        }

        val outputSize = (samples.size / speed).roundToInt().coerceAtLeast(1)
        return ShortArray(outputSize) { index ->
            val sourceIndex = index * speed
            val leftIndex = sourceIndex.toInt().coerceAtMost(samples.lastIndex)
            val rightIndex = (leftIndex + 1).coerceAtMost(samples.lastIndex)
            val fraction = sourceIndex - leftIndex
            val interpolated = samples[leftIndex] + ((samples[rightIndex] - samples[leftIndex]) * fraction)
            interpolated.roundToInt().toShort()
        }
    }

    private fun encodeWav(samples: ShortArray, sampleRate: Int): ByteArray {
        val format = AudioFormat(sampleRate.toFloat(), 16, 1, true, false)
        val byteBuffer = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        samples.forEach { byteBuffer.putShort(it) }
        val byteArray = byteBuffer.array()
        ByteArrayInputStream(byteArray).use { input ->
            val audioInputStream = AudioInputStream(input, format, samples.size.toLong())
            ByteArrayOutputStream().use { output ->
                AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, output)
                return output.toByteArray()
            }
        }
    }

    private fun encodePcm(samples: ShortArray): ByteArray {
        val byteBuffer = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        samples.forEach { byteBuffer.putShort(it) }
        return byteBuffer.array()
    }

    data class VoiceSpec(
        val modelPath: Path,
        val configPath: Path,
        val sampleRate: Int
    ) {
        val cacheKey: String = "${modelPath.toAbsolutePath().normalize()}|${configPath.toAbsolutePath().normalize()}"
    }
}
