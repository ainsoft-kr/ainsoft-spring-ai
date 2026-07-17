# Spring AI Chat API

🌐 **Language** · [한국어](README.ko.md) · **English**

---

Kotlin-based Spring Boot project demonstrating how to expose AI-powered endpoints backed by Spring AI's `ChatClient`. Covers text chat, multimodal input, OCR extraction, text-to-speech, retrieval-augmented generation, content moderation, and streaming responses — all wired to a swappable model backend (Ollama, OpenAI, or OCI GenAI).

## Requirements

- Java 24 (configured via Gradle toolchain)
- Ollama server running locally (or accessible) with models: `gemma2` (chat) and `nomic-embed-text` (embeddings)
- Optional: An OpenAI-compatible API key exposed through `OPENAI_API_KEY` if using OpenAI models

## Running the Server

Pull the required Ollama models first:

```bash
ollama pull gemma2
ollama pull nomic-embed-text
```

Then start the application:

```bash
./gradlew bootRun
```

The server listens on `http://localhost:8080` by default. Swagger UI is available at `http://localhost:8080/swagger-ui/index.html`.

---

## API Endpoints

### Chat — `POST /api/chat`

Send a JSON body to get a text reply. The optional `voice` field controls the tone via the system prompt, and `conversationId` ties requests to the same memory window.

```bash
curl -X POST http://localhost:8080/api/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"What can you do for me today?","voice":"friendly"}'
```

```json
{
  "message": "I can help with documentation, coding, and brainstorming. What's next?",
  "voice": "friendly"
}
```

### Streaming Chat — `POST /api/chat/stream`

Returns a `text/event-stream` (Server-Sent Events) response, streaming tokens as they are generated.

```bash
curl -X POST http://localhost:8080/api/chat/stream \
  -H 'Content-Type: application/json' \
  -d '{"message":"Tell me a short story","voice":"neutral"}'
```

### Multimodal Input — `POST /api/chat/multimodal`

Pass an image, audio, or any supported media blob alongside a text prompt via a `multipart/form-data` request.

```bash
curl -X POST http://localhost:8080/api/chat/multimodal \
  -H 'Content-Type: multipart/form-data' \
  -F 'payload={"message":"Describe what you see","voice":"friendly"};type=application/json' \
  -F 'media=@/path/to/image.png;type=image/png'
```

### OCR Extraction — `POST /api/chat/ocr`

Instructs the model to act as an OCR engine and return the text content of the uploaded image.

```bash
curl -X POST http://localhost:8080/api/chat/ocr \
  -H 'Content-Type: multipart/form-data' \
  -F 'payload={"instructions":"Scan this receipt for the total amount","voice":"formal"};type=application/json' \
  -F 'media=@/path/to/receipt.png;type=image/png'
```

The response shape mirrors the regular chat response but includes an `extractedText` field.

### Chat Memory — `GET /api/chat/memory/{conversationId}`

Inspect the rolling message window stored for a conversation.

```bash
curl http://localhost:8080/api/chat/memory/my-session
```

### Clear Chat Memory — `DELETE /api/chat/memory/{conversationId}`

Wipe the stored messages for a conversation window.

```bash
curl -X DELETE http://localhost:8080/api/chat/memory/my-session
```

---

### Text-to-Speech — `POST /api/tts`

Generate audio from text using the local Piper TTS model. Supported formats are `wav` and `pcm`. Speed can range from `0.5` to `2.0`.

```bash
curl -X POST http://localhost:8080/api/tts \
  -H 'Content-Type: application/json' \
  -d '{"text":"Hello, this is a test message","voice":"default","format":"wav","speed":1.0}' \
  --output output.wav
```

Place the Piper model artifacts in `./data/model` and download them with:

```bash
make piper-model
```

This downloads:

- `./data/model/en_US-lessac-medium.onnx`
- `./data/model/en_US-lessac-medium.onnx.json`

---

### RAG — Documents & Query

#### Add Documents — `POST /api/rag/documents`

Ingest one or more text chunks into the vector store.

```bash
curl -X POST http://localhost:8080/api/rag/documents \
  -H 'Content-Type: application/json' \
  -d '[{"text":"Spring AI wraps vector stores with QuestionAnswerAdvisor","source":"docs"}]'
```

#### List Documents — `GET /api/rag/documents`

Inspect all indexed document summaries.

```bash
curl http://localhost:8080/api/rag/documents
```

#### Delete a Document — `DELETE /api/rag/documents/{id}`

Remove a document from the vector store and the index. Returns `204 No Content` on success, `404 Not Found` if the ID does not exist.

```bash
curl -X DELETE http://localhost:8080/api/rag/documents/doc-id-here
```

#### Query — `POST /api/rag/query`

Answer a question using context retrieved from the vector store.

```bash
curl -X POST http://localhost:8080/api/rag/query \
  -H 'Content-Type: application/json' \
  -d '{"question":"What does the QuestionAnswerAdvisor do?","topK":3}'
```

The response contains the assistant answer plus the retrieved document context.

---

## Content Moderation

The `ModerationAdvisor` intercepts every chat request before it reaches the model. It is **disabled by default**. Enable it in `application.yml`:

```yaml
moderation:
  enabled: true
  provider: keyword          # or "openai"
  blocked-keywords:
    - "disallowed word"
  # openai-api-key: ${OPENAI_API_KEY}
  # openai-base-url: https://api.openai.com
```

When `provider: openai`, each request is checked against the OpenAI `/v1/moderations` endpoint. Network failures use a **fail-open** strategy — the request passes through with a warning log. Blocked requests return `HTTP 403`.

---

## Configuration

| Key | Default | Description |
|---|---|---|
| `ai.provider` | `ollama` | Backend to use: `ollama`, `openai`, or `oci` |
| `spring.ai.ollama.chat.options.model` | `gemma2` | Ollama chat model name |
| `spring.ai.ollama.embedding.options.model` | `nomic-embed-text` | Ollama embedding model name |
| `chat.api.system-prompt` | _(see yml)_ | System prompt template. `{voice}` is replaced at runtime |
| `chat.api.default-voice` | `neutral` | Fallback voice when not specified in the request |
| `moderation.enabled` | `false` | Enable content moderation |
| `moderation.provider` | `keyword` | Moderation backend: `keyword` or `openai` |
| `rag.store-path` | `./data/rag/vector-store.json` | Path to persist the vector store |
| `rag.document-index-path` | `./data/rag/documents.json` | Path to persist the document index |
| `tts.model-path` | `./data/model` | Directory containing Piper `.onnx` and `.json` files |

### OpenAI-Compatible Backend

```bash
./gradlew bootRun --args='--ai.provider=openai \
  --spring.ai.model.chat=openai \
  --spring.ai.model.embedding=openai \
  --spring.ai.openai.api-key=${OPENAI_API_KEY} \
  --spring.ai.openai.base-url=http://localhost:8317/v1 \
  --spring.ai.openai.chat.completions-path=/chat/completions \
  --spring.ai.openai.chat.options.model=gemini-2.5-flash'
```

### OCI GenAI Backend

Set `ai.provider=oci` and add the following to `application.yml`:

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

## Model Coverage

| Category | Implementation |
|---|---|
| **Chat** | Ollama (`gemma2`) · OpenAI-compatible · OCI GenAI Cohere |
| **Embeddings** | Ollama (`nomic-embed-text`) · OpenAI |
| **Multimodal** | Depends on the configured chat model's media support |
| **TTS** | Local Piper JNI — no external API required |
| **Moderation** | OpenAI `/v1/moderations` · Keyword blocklist |
| **Memory** | `MessageWindowChatMemory` (in-process, rolling window) |
| **RAG** | `SimpleVectorStore` + `QuestionAnswerAdvisor` |

---

More information is available in the [Spring AI getting started guide](https://docs.spring.io/spring-ai/reference/getting-started.html).
