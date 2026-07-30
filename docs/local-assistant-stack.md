# Local Personal Assistant Stack

This project is being shaped as a local-first assistant with Spring Boot as the main controller. The first implementation can run as one local process, while the package boundaries should map cleanly to future microservices:

- `assistant-api`: HTTP, WebSocket, and UI endpoints.
- `voice-service`: microphone audio intake, STT command execution, and TTS playback.
- `model-router`: routes simple/private work to Ollama and complex/cloud work to Gemini.
- `rag-ingestion`: uploads, parses, chunks, embeds, and stores documents in batches.
- `desktop-tools`: opens, closes, and inspects local system applications.
- `knowledge-store`: MySQL metadata plus Qdrant vector search.

## Local Software

- Java 21 LTS.
- Maven Wrapper from this repo: `sh mvnw ...`.
- Docker Desktop or another Compose-compatible runtime.
- MySQL 8.4 for relational data.
- Qdrant for the local vector database.
- Ollama for local Llama chat and local embedding models.
- Gemini API key in `GEMINI_AI_KEY` for cloud LLM routing.
- `whisper.cpp` with a `whisper-cli` command available on `PATH` for local speech-to-text.
- Piper with a `piper` command available on `PATH` for local text-to-speech.
- macOS LaunchAgent for startup on this machine. Linux can use `systemd`; Windows can use Task Scheduler.

## Maven Capabilities Added

- Spring Boot MVC, Thymeleaf, and WebSocket for the local UI and live voice/response channel.
- Spring Batch for controlled PDF/text ingestion and embedding batches.
- Spring Data JPA, MySQL Connector/J, and Flyway for metadata and schema management.
- LangChain4j core, Spring Boot 4 integration, Ollama, Gemini, Qdrant, Tika, PDFBox, and Micrometer metrics.
- OSHI and Commons IO for desktop/system integration and file handling.
- H2 and Spring Batch test support for local tests without requiring MySQL.

## Runtime Services

Start MySQL and Qdrant:

```bash
docker compose up -d mysql qdrant
```

Install local models:

```bash
ollama pull llama3.1
ollama pull nomic-embed-text
```

Run the app:

```bash
export GEMINI_AI_KEY=your_key_here
sh mvnw spring-boot:run
```

The app defaults to:

- UI/API: `http://localhost:8080`
- MySQL: `localhost:3306`
- Qdrant HTTP: `localhost:6333`
- Qdrant gRPC: `localhost:6334`
- Ollama: `http://localhost:11434`

## RAG Upload Flow

1. Upload PDF, text, or Markdown through the UI.
2. Store the original file under `~/.mypersonalassist/uploads`.
3. Parse files with LangChain4j document parsers backed by Tika/PDFBox.
4. Split text with configured chunk size and overlap.
5. Embed chunks in batches controlled by:
   - `assistant.rag.embedding-batch-size`
   - `assistant.rag.embedding-batch-delay`
   - `assistant.rag.max-parallel-files`
6. Store embeddings and metadata in Qdrant.
7. Store upload metadata, indexing status, and command history in MySQL.

## Startup

After packaging the app with `sh mvnw package`, copy `ops/macos/com.mypersonalassist.launchagent.plist.template` to:

```bash
~/Library/LaunchAgents/com.mypersonalassist.plist
```

Then load it with:

```bash
launchctl load ~/Library/LaunchAgents/com.mypersonalassist.plist
```

Add `GEMINI_AI_KEY` to the LaunchAgent environment only if cloud routing should be available at startup.

## Useful References

- LangChain4j Spring Boot integration: https://docs.langchain4j.dev/tutorials/spring-boot-integration/
- LangChain4j Gemini integration: https://docs.langchain4j.dev/integrations/language-models/google-ai-gemini/
- LangChain4j Gemini embeddings: https://docs.langchain4j.dev/integrations/embedding-models/google-ai-gemini/
- LangChain4j Ollama integration: https://docs.langchain4j.dev/integrations/language-models/ollama/
- LangChain4j Qdrant integration: https://docs.langchain4j.dev/integrations/embedding-stores/qdrant/
