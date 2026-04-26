package com.ainsoft.ai.controller

import com.ainsoft.ai.dto.TextToSpeechRequest
import com.ainsoft.ai.service.TextToSpeechService
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/tts")
class TextToSpeechController(
    private val textToSpeechService: TextToSpeechService
) {

    @PostMapping
    fun synthesize(@Valid @RequestBody request: TextToSpeechRequest): ResponseEntity<ByteArray> {
        val (audio, mediaType) = textToSpeechService.synthesize(request)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, mediaType.toString())
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"tts.${mediaType.subtype}\"")
            .body(audio)
    }
}
