# Spring AI Chat API

Small Kotlin-based Spring Boot project demonstrating how to expose a simple chat completion endpoint backed by Spring AI's `ChatClient`.

## Requirements

- Java 24 (configured via Gradle toolchain)
- Ollama server running locally (or accessible) with models: `gemma2` (chat) and `nomic-embed-text` (embeddings)
- Optional: An OpenAI-compatible API key exposed through `OPENAI_API_KEY` if using OpenAI models

## Running the server

First, pull the required Ollama models:

```bash
ollama pull gemma2
ollama pull nomic-embed-text
```

Then start the application:

```bash
./gradlew bootRun
```

The application listens on `http://localhost:8080/api/chat` by default.

## Calling the Chat Endpoint

Send a `POST` with JSON to `/api/chat`:

```bash
curl -X POST http://localhost:8080/api/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"What can you do for me today?","voice":"friendly"}'
```

The response returns the assistant text plus the voice that was used:

```json
{
  "message": "I can help with documentation, coding, and brainstorming. What's next?",
  "voice": "friendly"
}
```

## Multimodal Input

When you want the LLM to reason about non-text inputs, send a multipart request to `/api/chat/multimodal` containing the JSON payload and the media blob (images, audio, etc.). The same system prompt controls how the assistant responds and the `{voice}` placeholder is filled with the requested voice.

```bash
curl -X POST http://localhost:8080/api/chat/multimodal \
  -H 'Content-Type: multipart/form-data' \
  -F 'payload={"message":"Describe what you see","voice":"friendly"};type=application/json' \
  -F 'media=@/path/to/image.png;type=image/png'
```

The endpoint returns the assistant text plus the voice in the same shape as the regular chat endpoint.

## OCR Extraction

Uploading an image to `/api/chat/ocr` instructs the configured `ChatModel` to act like an OCR engine and extract the textual content. Supply a short `instructions` prompt to shape how the assistant interprets the media.

```bash
curl -X POST http://localhost:8080/api/chat/ocr \
  -H 'Content-Type: multipart/form-data' \
  -F 'payload={"instructions":"Scan this receipt for the total amount","voice":"formal"};type=application/json' \
  -F 'media=@/path/to/receipt.png;type=image/png'
```

The response mirrors the existing chat shape but the `extractedText` field holds the OCR-style transcription along with the `voice` that was used.

## Text-to-Speech (TTS)

Send a POST request to `/api/tts` to generate audio from text using the Piper model. Specify the text content, voice, output format, and playback speed in the JSON payload.

```bash
curl -X POST http://localhost:8080/api/tts \
  -H 'Content-Type: application/json' \
  -d '{"text":"Hello, this is a test message","voice":"default","format":"wav","speed":1.0}' \
  --output output.wav
```

The endpoint returns the audio file saved to the specified output filename. Place the Piper model artifacts in `./data/model` and download them with:

```bash
make piper-model
```

This downloads:

- `./data/model/en_US-lessac-medium.onnx`
- `./data/model/en_US-lessac-medium.onnx.json`

Chat endpoints accept an optional `conversationId` in the JSON payload. When you pass the same ID a few times, the `ChatMemory` advisor will include the rolling window in future prompts, and you can inspect what the model retained via `GET /api/chat/memory/{conversationId}`.

## Configuration

- `spring.ai.ollama.chat.options.model`: defaults to `gemma2` for chat completions
- `spring.ai.ollama.embedding.options.model`: defaults to `nomic-embed-text` for embeddings
- `ai.provider`: selects the provider-specific `ChatModel` and `EmbeddingModel` beans. Supported values are `ollama`, `openai`, and `oci`.
- `chat.api.system-prompt`: template for the system message. The `{voice}` placeholder is replaced with the requested voice (defaults to `neutral`).

For OpenAI-compatible configuration (optional):
- `spring.ai.openai.api-key`: your OpenAI API key
- `spring.ai.openai.base-url`: the API base URL. If it includes `/v1`, set `spring.ai.openai.chat.completions-path=/chat/completions`.
- `spring.ai.openai.chat.options.model`: the chat model ID.

Spring AI exposes the shared [ChatModel API](https://docs.spring.io/spring-ai/reference/api/chatmodel.html) so you can swap providers without changing your business logic.

## Model Coverage

The project wires the following Spring AI categories so you can explore the combination that fits your needs:

- **Chat**: Ollama chat completions via `spring-ai-starter-model-ollama` using `gemma2` (default, used by `/api/chat`).
- **Embeddings**: Ollama embeddings via `nomic-embed-text` for semantic search and RAG capabilities.
- **Image & Audio**: Multimodal chat depends on the configured chat model's media support. OpenAI image/audio capabilities are available once the OpenAI starter is enabled; local TTS uses Piper.
- **Moderation**: OpenAI's moderation APIs are approachable through the same configuration, letting you guard calls before they hit the generative endpoints.

### Optional: OpenAI-Compatible Models
The OpenAI starter is included. To use an OpenAI-compatible local factory instead of Ollama, start the app with:

```bash
./gradlew bootRun --args='--ai.provider=openai --spring.ai.model.chat=openai --spring.ai.model.embedding=openai --spring.ai.openai.api-key=${OPENAI_API_KEY} --spring.ai.openai.base-url=http://localhost:8317/v1 --spring.ai.openai.chat.completions-path=/chat/completions --spring.ai.openai.chat.options.model=gemini-2.5-flash'
```

That model selection is compatible with the existing controller/service because they speak the same `ChatModel` contract.

### OCR with OCI GenAI Cohere
You can also plug a paid or free OCI GenAI Cohere model to perform OCR-style extractive reasoning. Add the OCI GenAI starter only when you plan to hit Oracle's REST endpoint instead of the default local Ollama setup.

```groovy
implementation 'org.springframework.ai:spring-ai-starter-model-oci-genai'
```

Then provide your credentials and model configuration in `application.properties` (replace the placeholders with real values):

```properties
spring.ai.model.chat=oci-genai
spring.ai.oci.genai.authenticationType=file
spring.ai.oci.genai.file=/path/to/.oci/config
spring.ai.oci.genai.cohere.chat.options.compartment=ocid1.compartment.ocxxxx
spring.ai.oci.genai.cohere.chat.options.model=ocid1.model.ocxxxx
```

The `/api/chat/ocr` endpoint sends the provided media to whichever `ChatModel` is configured and asks it to return the recognized text. The service also exposes `/api/chat/multimodal` for richer captioning flows that reuse the same prompt logic.

### Chat Memory Support

Spring AI automatically wires a `ChatMemory` implementation (`MessageWindowChatMemory` by default) when you include `spring-ai-starter-model-chat-memory`. The controller exposes `/api/chat/memory/{conversationId}` to peek at the remembered messages, and chat payloads accept an optional `conversationId` to keep the memory keyed to your user's session.

### Retrieval-Augmented Generation (RAG)

Spring AI already ships with RAG helpers, so the project pulls in `spring-ai-advisors-vector-store`, `spring-ai-rag`, and `spring-ai-vector-store` to build a minimal vector store + advisor pipeline. Use `POST /api/rag/documents` to add documents (text + metadata), `GET /api/rag/documents` to inspect what is stored, and `POST /api/rag/query` to answer questions with context drawn from the indexed documents.

```bash
curl -X POST http://localhost:8080/api/rag/documents \
  -H 'Content-Type: application/json' \
  -d '[{"text":"Spring AI wraps vector stores with QuestionAnswerAdvisor","source":"docs"}]'

curl -X POST http://localhost:8080/api/rag/query \
  -H 'Content-Type: application/json' \
  -d '{"question":"What does the QuestionAnswerAdvisor do?","topK":3}'
```

The `/api/rag/query` response contains the assistant answer plus the vector-store context so you can see which documents were retrieved.

More information on Spring AI chat tooling is available in the [official getting started guide](https://docs.spring.io/spring-ai/reference/getting-started.html).
