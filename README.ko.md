# Spring AI Chat API

🌐 **언어** · **한국어** · [English](README.md)

---

Spring AI의 `ChatClient`를 기반으로 다양한 AI 엔드포인트를 노출하는 Kotlin + Spring Boot 프로젝트입니다. 텍스트 채팅, 멀티모달 입력, OCR 추출, 텍스트 음성 변환(TTS), 검색 증강 생성(RAG), 콘텐츠 검수(Moderation), 스트리밍 응답을 모두 지원하며, 백엔드 모델은 Ollama·OpenAI·OCI GenAI 중 자유롭게 전환할 수 있습니다.

## 요구사항

- Java 24 (Gradle 툴체인으로 설정)
- 로컬(또는 접근 가능한) Ollama 서버와 모델: `gemma2`(채팅), `nomic-embed-text`(임베딩)
- 선택: OpenAI 호환 모델 사용 시 `OPENAI_API_KEY` 환경 변수

## 서버 실행

먼저 필요한 Ollama 모델을 내려받습니다:

```bash
ollama pull gemma2
ollama pull nomic-embed-text
```

애플리케이션을 시작합니다:

```bash
./gradlew bootRun
```

서버는 기본적으로 `http://localhost:8080`에서 실행됩니다. Swagger UI는 `http://localhost:8080/swagger-ui/index.html`에서 확인할 수 있습니다.

---

## API 엔드포인트

### 채팅 — `POST /api/chat`

JSON 본문을 전송하면 텍스트 응답을 반환합니다. `voice` 필드로 어조를 조절하고, `conversationId`를 지정하면 같은 메모리 창에 대화가 유지됩니다.

```bash
curl -X POST http://localhost:8080/api/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"오늘 뭘 도와줄 수 있어?","voice":"friendly"}'
```

```json
{
  "message": "문서 작성, 코딩, 브레인스토밍을 도와드릴 수 있어요. 무엇을 시작할까요?",
  "voice": "friendly"
}
```

### 스트리밍 채팅 — `POST /api/chat/stream`

토큰이 생성되는 즉시 `text/event-stream`(Server-Sent Events)으로 응답을 스트리밍합니다.

```bash
curl -X POST http://localhost:8080/api/chat/stream \
  -H 'Content-Type: application/json' \
  -d '{"message":"짧은 이야기를 들려줘","voice":"neutral"}'
```

### 멀티모달 입력 — `POST /api/chat/multimodal`

이미지, 오디오 등 미디어 파일을 텍스트 프롬프트와 함께 `multipart/form-data`로 전송합니다.

```bash
curl -X POST http://localhost:8080/api/chat/multimodal \
  -H 'Content-Type: multipart/form-data' \
  -F 'payload={"message":"이 이미지를 설명해줘","voice":"friendly"};type=application/json' \
  -F 'media=@/path/to/image.png;type=image/png'
```

### OCR 추출 — `POST /api/chat/ocr`

모델이 OCR 엔진처럼 동작하여 업로드된 이미지에서 텍스트를 추출합니다.

```bash
curl -X POST http://localhost:8080/api/chat/ocr \
  -H 'Content-Type: multipart/form-data' \
  -F 'payload={"instructions":"영수증에서 합계 금액을 찾아줘","voice":"formal"};type=application/json' \
  -F 'media=@/path/to/receipt.png;type=image/png'
```

응답은 일반 채팅과 같은 형태이며, `extractedText` 필드에 추출된 텍스트가 담깁니다.

### 대화 메모리 조회 — `GET /api/chat/memory/{conversationId}`

특정 대화에 저장된 메시지 창을 확인합니다.

```bash
curl http://localhost:8080/api/chat/memory/my-session
```

### 대화 메모리 삭제 — `DELETE /api/chat/memory/{conversationId}`

특정 대화의 저장된 메시지를 초기화합니다.

```bash
curl -X DELETE http://localhost:8080/api/chat/memory/my-session
```

---

### 텍스트 음성 변환(TTS) — `POST /api/tts`

로컬 Piper 모델을 사용해 텍스트에서 오디오를 생성합니다. 지원 포맷은 `wav`와 `pcm`이며, 재생 속도는 `0.5`~`2.0` 범위로 지정할 수 있습니다.

```bash
curl -X POST http://localhost:8080/api/tts \
  -H 'Content-Type: application/json' \
  -d '{"text":"안녕하세요, 테스트 메시지입니다","voice":"default","format":"wav","speed":1.0}' \
  --output output.wav
```

Piper 모델 파일을 `./data/model`에 배치하고 아래 명령으로 다운로드합니다:

```bash
make piper-model
```

다운로드되는 파일:

- `./data/model/en_US-lessac-medium.onnx`
- `./data/model/en_US-lessac-medium.onnx.json`

---

### RAG — 문서 관리 및 질의

#### 문서 추가 — `POST /api/rag/documents`

텍스트 청크를 벡터 스토어에 인제스트합니다.

```bash
curl -X POST http://localhost:8080/api/rag/documents \
  -H 'Content-Type: application/json' \
  -d '[{"text":"Spring AI는 QuestionAnswerAdvisor로 벡터 스토어를 감쌉니다","source":"docs"}]'
```

#### 문서 목록 조회 — `GET /api/rag/documents`

인덱싱된 모든 문서 요약을 조회합니다.

```bash
curl http://localhost:8080/api/rag/documents
```

#### 문서 삭제 — `DELETE /api/rag/documents/{id}`

벡터 스토어와 인덱스에서 문서를 삭제합니다. 성공 시 `204 No Content`, 존재하지 않으면 `404 Not Found`를 반환합니다.

```bash
curl -X DELETE http://localhost:8080/api/rag/documents/doc-id-here
```

#### 질의 — `POST /api/rag/query`

벡터 스토어에서 검색한 컨텍스트를 기반으로 질문에 답합니다.

```bash
curl -X POST http://localhost:8080/api/rag/query \
  -H 'Content-Type: application/json' \
  -d '{"question":"QuestionAnswerAdvisor는 어떤 역할을 하나요?","topK":3}'
```

응답에는 모델 답변과 검색된 문서 컨텍스트가 함께 포함됩니다.

---

## 콘텐츠 검수 (Moderation)

`ModerationAdvisor`는 모든 채팅 요청이 모델에 전달되기 전에 콘텐츠를 검사합니다. **기본값은 비활성화**입니다. `application.yml`에서 활성화할 수 있습니다:

```yaml
moderation:
  enabled: true
  provider: keyword          # 또는 "openai"
  blocked-keywords:
    - "금지어 예시"
  # openai-api-key: ${OPENAI_API_KEY}
  # openai-base-url: https://api.openai.com
```

`provider: openai`로 설정하면 OpenAI `/v1/moderations` 엔드포인트를 호출합니다. 네트워크 오류 시에는 **fail-open** 전략을 적용해 경고만 남기고 요청을 통과시킵니다. 차단된 요청은 `HTTP 403`을 반환합니다.

---

## 설정 레퍼런스

| 키 | 기본값 | 설명 |
|---|---|---|
| `ai.provider` | `ollama` | 사용할 백엔드: `ollama`, `openai`, `oci` |
| `spring.ai.ollama.chat.options.model` | `gemma2` | Ollama 채팅 모델 이름 |
| `spring.ai.ollama.embedding.options.model` | `nomic-embed-text` | Ollama 임베딩 모델 이름 |
| `chat.api.system-prompt` | _(yml 참고)_ | 시스템 프롬프트 템플릿. `{voice}`가 런타임에 치환됨 |
| `chat.api.default-voice` | `neutral` | 요청에 voice 미지정 시 기본값 |
| `moderation.enabled` | `false` | 콘텐츠 검수 활성화 여부 |
| `moderation.provider` | `keyword` | 검수 백엔드: `keyword` 또는 `openai` |
| `rag.store-path` | `./data/rag/vector-store.json` | 벡터 스토어 저장 경로 |
| `rag.document-index-path` | `./data/rag/documents.json` | 문서 인덱스 저장 경로 |
| `tts.model-path` | `./data/model` | Piper `.onnx` 및 `.json` 파일 디렉토리 |

### OpenAI 호환 백엔드 사용

```bash
./gradlew bootRun --args='--ai.provider=openai \
  --spring.ai.model.chat=openai \
  --spring.ai.model.embedding=openai \
  --spring.ai.openai.api-key=${OPENAI_API_KEY} \
  --spring.ai.openai.base-url=http://localhost:8317/v1 \
  --spring.ai.openai.chat.completions-path=/chat/completions \
  --spring.ai.openai.chat.options.model=gemini-2.5-flash'
```

### OCI GenAI 백엔드 사용

`ai.provider=oci`로 설정하고 `application.yml`에 다음을 추가합니다:

```yaml
spring:
  ai:
    oci:
      genai:
        authenticationType: file
        file: /path/to/.oci/config
        cohere:
          chat:
            options:
              compartment: ocid1.compartment.ocxxxx
              model: ocid1.model.ocxxxx
```

---

## 지원 모델 범위

| 카테고리 | 구현 내용 |
|---|---|
| **채팅** | Ollama (`gemma2`) · OpenAI 호환 · OCI GenAI Cohere |
| **임베딩** | Ollama (`nomic-embed-text`) · OpenAI |
| **멀티모달** | 설정된 채팅 모델의 미디어 지원 여부에 따름 |
| **TTS** | 로컬 Piper JNI — 외부 API 불필요 |
| **Moderation** | OpenAI `/v1/moderations` · 키워드 차단 목록 |
| **대화 메모리** | `MessageWindowChatMemory` (인메모리, 롤링 윈도우) |
| **RAG** | `SimpleVectorStore` + `QuestionAnswerAdvisor` |

---

더 자세한 내용은 [Spring AI 공식 시작 가이드](https://docs.spring.io/spring-ai/reference/getting-started.html)를 참고하세요.
