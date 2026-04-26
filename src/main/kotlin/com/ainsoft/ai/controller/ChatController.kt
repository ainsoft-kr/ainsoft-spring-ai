package com.ainsoft.ai.controller

import com.ainsoft.ai.dto.ChatMemorySnapshot
import com.ainsoft.ai.dto.ChatRequest
import com.ainsoft.ai.dto.ChatResult
import com.ainsoft.ai.dto.MultimodalRequest
import com.ainsoft.ai.dto.OcrRequest
import com.ainsoft.ai.dto.OcrResult
import com.ainsoft.ai.service.ChatService

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/chat")
class ChatController(
    private val chatService: ChatService
) {

    @PostMapping
    fun prompt(@Valid @RequestBody request: ChatRequest): ResponseEntity<ChatResult> {
        return ResponseEntity.ok(chatService.ask(request))
    }

    @PostMapping("/multimodal")
    fun promptWithMedia(
        @Valid @RequestPart("payload") request: MultimodalRequest,
        @RequestPart("media", required = false) media: MultipartFile?
    ): ResponseEntity<ChatResult> {
        return ResponseEntity.ok(chatService.askWithMedia(request, media))
    }

    @PostMapping("/ocr")
    fun ocr(
        @Valid @RequestPart("payload") request: OcrRequest,
        @RequestPart("media") media: MultipartFile
    ): ResponseEntity<OcrResult> {
        return ResponseEntity.ok(chatService.extractText(media, request))
    }

    @GetMapping("/memory/{conversationId}")
    fun memory(@PathVariable conversationId: String): ResponseEntity<ChatMemorySnapshot> {
        return ResponseEntity.ok(chatService.history(conversationId))
    }
}
