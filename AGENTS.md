# Repository Agent Rules

These rules are part of the working contract for this repository. Keep context small, prefer indexed or structured MCP tools first, and only fall back to broad shell reads when the MCP answer is missing, stale, or too narrow.

## MCP Priority

- Codebase questions: use Symdex first. Prefer `get_repo_outline`, `search_symbols`, `get_file_outline`, `get_symbol`, `search_routes`, and `build_context_pack` before reading whole files.
- Framework and library questions: use Context7 first for Spring Boot, Spring AI, Kotlin, Gradle, and dependency behavior. Use web search only when the user asks for latest/current information or official docs are insufficient.
- API surface questions: use the `openapi_spring_ai` MCP first for endpoint paths, schemas, and request/response shapes. Avoid dumping the full OpenAPI JSON into context.
- Persistent project decisions: use Memory MCP for stable decisions that should survive sessions. Do not store secrets, API keys, credentials, or personal data.
- Git state: use Git MCP when available for status, diffs, and history summaries. Use shell `git` commands as a fallback.
- Browser and UI checks: use Playwright MCP only for browser-visible behavior such as Swagger UI or real page interaction. Prefer `curl`, MockMvc, or Gradle tests for API behavior.
- Filesystem MCP is for targeted reads and directory checks. Do not use it to bulk-load the repository.

## Token Budget Rules

- Start with the smallest tool that can answer the question. Ask Symdex for outlines or symbols before opening files.
- Avoid full-file dumps unless the file is already known to be small or the exact range is needed.
- Avoid broad commands such as unfiltered `rg --files`, recursive `ls`, or long build logs. Add filters and cap output.
- For source reads, prefer symbol-level or route-level context over `sed -n '1,220p'`.
- When tests fail, include only the failing task, assertion, stack frame, and nearby application logs needed to diagnose it.
- After large source changes, invalidate or re-index Symdex before relying on indexed results.

## Project Commands

- Test: `./gradlew test`
- Build: `./gradlew build`
- Run API: `./gradlew bootRun`

## Fallbacks

If an MCP server is unavailable, stale, or too imprecise, use targeted shell commands. Keep the output narrow and explain the fallback briefly in the work summary when it matters.
