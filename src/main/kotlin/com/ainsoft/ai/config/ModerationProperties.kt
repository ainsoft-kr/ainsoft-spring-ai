package com.ainsoft.ai.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Moderation 기능 설정.
 *
 * - enabled: true 시 채팅 요청마다 콘텐츠 검수를 수행한다.
 * - provider: "openai" (OpenAI /v1/moderations 호출) 또는 "keyword" (내장 키워드 필터).
 * - openaiApiKey: provider=openai 일 때 사용할 API 키. 미설정 시 spring.ai.openai.api-key 를 우선 사용.
 * - openaiBaseUrl: OpenAI-compatible 엔드포인트 기본 URL.
 * - blockedKeywords: provider=keyword 일 때 차단할 단어 목록.
 */
@ConfigurationProperties(prefix = "moderation")
class ModerationProperties {
    var enabled: Boolean = false
    var provider: String = "keyword"
    var openaiApiKey: String = ""
    var openaiBaseUrl: String = "https://api.openai.com"
    var blockedKeywords: List<String> = emptyList()
}
