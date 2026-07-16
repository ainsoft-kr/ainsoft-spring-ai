package com.ainsoft.ai.advisor

import com.ainsoft.ai.config.ModerationProperties
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClientRequest
import org.springframework.ai.chat.client.ChatClientResponse
import org.springframework.ai.chat.client.advisor.api.CallAdvisor
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain
import org.springframework.ai.chat.messages.MessageType
import org.springframework.web.client.RestClient

/**
 * 채팅 요청이 모델에 전달되기 전에 콘텐츠를 검수하는 Advisor.
 *
 * - provider=openai: OpenAI `/v1/moderations` 엔드포인트 호출
 * - provider=keyword: 설정된 blockedKeywords 목록과 대조 (SafeGuardAdvisor와 동일 전략, 추가 기능 포함)
 *
 * 위반이 감지되면 [ModerationException]을 던져 요청을 차단한다.
 * 네트워크 오류 등 외부 실패는 fail-open 전략으로 경고만 남기고 통과시킨다.
 */
class ModerationAdvisor(
    private val properties: ModerationProperties
) : CallAdvisor {

    private val log = LoggerFactory.getLogger(ModerationAdvisor::class.java)

    private val restClient: RestClient by lazy {
        RestClient.builder()
            .baseUrl(properties.openaiBaseUrl)
            .defaultHeader("Authorization", "Bearer ${properties.openaiApiKey}")
            .defaultHeader("Content-Type", "application/json")
            .build()
    }

    override fun getName(): String = "ModerationAdvisor"

    /** 가장 먼저 실행되도록 최고 우선순위 */
    override fun getOrder(): Int = Int.MIN_VALUE

    override fun adviseCall(
        request: ChatClientRequest,
        chain: CallAdvisorChain
    ): ChatClientResponse {
        val userText = extractUserText(request)
        if (!userText.isNullOrBlank()) {
            moderate(userText)
        }
        return chain.nextCall(request)
    }

    private fun extractUserText(request: ChatClientRequest): String? {
        val prompt = request.prompt()
        // getUserMessage()로 가장 최근 USER 메시지 텍스트를 우선 반환
        val userMsg = prompt.getUserMessage()?.let { it.text?.takeIf { t -> t.isNotBlank() } }
        if (userMsg != null) return userMsg
        // 전체 메시지 목록에서 USER 타입 텍스트를 합산
        return prompt.getInstructions()
            .filter { it.messageType == MessageType.USER }
            .mapNotNull { it.text }
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .takeIf { it.isNotBlank() }
    }

    private fun moderate(text: String) {
        when (properties.provider.lowercase()) {
            "openai" -> moderateWithOpenAi(text)
            "keyword" -> moderateWithKeywords(text)
            else -> log.warn("Unknown moderation provider '{}', skipping check.", properties.provider)
        }
    }

    // -------------------------------------------------------------------------
    // OpenAI /v1/moderations
    // -------------------------------------------------------------------------

    private fun moderateWithOpenAi(text: String) {
        try {
            val response = restClient.post()
                .uri("/v1/moderations")
                .body(mapOf("input" to text))
                .retrieve()
                .body(ModerationApiResponse::class.java)
                ?: return

            val flagged = response.results.any { it.flagged }
            if (flagged) {
                val categories = response.results
                    .flatMap { result ->
                        result.categories.entries.filter { it.value }.map { it.key }
                    }
                    .distinct()

                log.warn("Content flagged by OpenAI moderation. categories={}", categories)
                throw ModerationException("Request blocked: content violates usage policy. Categories: $categories")
            }
        } catch (ex: ModerationException) {
            throw ex
        } catch (ex: Exception) {
            // 네트워크 오류 등 외부 실패 시에는 경고만 남기고 통과 (fail-open)
            log.warn("OpenAI moderation check failed — failing open. reason={}", ex.message)
        }
    }

    // -------------------------------------------------------------------------
    // 키워드 필터
    // -------------------------------------------------------------------------

    private fun moderateWithKeywords(text: String) {
        val lowerText = text.lowercase()
        val matched = properties.blockedKeywords.firstOrNull { keyword ->
            keyword.isNotBlank() && lowerText.contains(keyword.lowercase())
        }
        if (matched != null) {
            log.warn("Content blocked by keyword filter. keyword={}", matched)
            throw ModerationException("Request blocked: content contains disallowed keyword.")
        }
    }

    // -------------------------------------------------------------------------
    // DTO
    // -------------------------------------------------------------------------

    data class ModerationApiResponse(
        val id: String = "",
        val model: String = "",
        val results: List<ModerationResult> = emptyList()
    )

    data class ModerationResult(
        val flagged: Boolean = false,
        val categories: Map<String, Boolean> = emptyMap()
    )
}

/** 콘텐츠 검수에 실패했을 때 던져지는 예외. */
class ModerationException(message: String) : RuntimeException(message)
