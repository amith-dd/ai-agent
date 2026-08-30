# RAG-Chat Integration - Quick Start Guide

## What Changed?

Your AI assistant now:
1. ✅ **Uses uploaded documents to answer questions** - No longer ignores files
2. ✅ **Shows which documents were used** - See source attribution in responses
3. ✅ **Manages uploaded files** - List and delete files via API
4. ✅ **Maintains chat history** - Uses conversation context + document context

## How to Use It

### 1. Upload Documents
```bash
# Upload one or more text/markdown files
curl -X POST \
  -F "files=@document.txt" \
  -F "files=@guide.md" \
  http://localhost:8080/api/rag/files

# Response:
# {
#   "fileStatuses": [
#     {"fileName":"document.txt","status":"stored","chunks":12},
#     {"fileName":"guide.md","status":"stored","chunks":8}
#   ]
# }
```

### 2. Ask Questions
```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "conversationId": null,
    "message": "Summarize the documents"
  }'

# Response includes:
# {
#   "assistantMessage": {
#     "content": "Based on document.txt and guide.md...",
#     "role": "ASSISTANT"
#   },
#   "citedFiles": ["document.txt", "guide.md"]
# }
```

### 3. View Uploaded Files
```bash
curl http://localhost:8080/api/rag/files

# Response:
# [
#   {
#     "id": 1,
#     "fileName": "document.txt",
#     "fileSize": 5242880,
#     "chunksCount": 12,
#     "ingestedAt": "2026-08-30T20:45:00Z"
#   }
# ]
```

### 4. Delete a File
```bash
curl -X DELETE http://localhost:8080/api/rag/files/1
```

## Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│                   User Upload Files                 │
└─────────────────────────────────────────────────────┘
          ↓
┌─────────────────────────────────────────────────────┐
│            RagService: Process Files                │
│  ├─ Extract text                                    │
│  ├─ Split into chunks (800 tokens)                 │
│  ├─ Generate embeddings (Ollama/Gemini)            │
│  ├─ Store in Qdrant (vector DB)                    │
│  └─ Save metadata to MySQL                         │
└─────────────────────────────────────────────────────┘
          ↓
        When User Chats:
          ↓
┌─────────────────────────────────────────────────────┐
│         ChatService: Integrate RAG                  │
│  ├─ Get user message                               │
│  ├─ Search Qdrant for similar docs                 │
│  ├─ Augment LLM prompt with docs                   │
│  ├─ Call Ollama/Gemini                             │
│  └─ Return response + citations                    │
└─────────────────────────────────────────────────────┘
```

## Key Components

### New Classes
| Class | Purpose |
|-------|---------|
| `RagQueryService` | Searches documents in Qdrant |
| `RagFileEntity` | Database entity for uploaded files |
| `RagFileRepository` | Spring Data JPA for file queries |
| `RetrievedDocument` | DTO for search results |
| `RagFileResponseDto` | DTO for file listing |

### Modified Classes
| Class | Changes |
|-------|---------|
| `ChatService` | Added RAG search before LLM call |
| `LocalLlamaClient` | Added RAG context to system prompt |
| `ChatResponseDto` | Added `citedFiles` field |
| `RagService` | Added file metadata saving + deletion |
| `RagRestController` | Added GET /files and DELETE /files/{id} |

## Database

### New Table: rag_files
```sql
CREATE TABLE rag_files (
    id BIGINT PRIMARY KEY,
    file_name VARCHAR(255),
    file_path VARCHAR(512) UNIQUE,
    file_size BIGINT,
    content_type VARCHAR(100),
    chunks_count INT,
    ingested_at TIMESTAMP,
    INDEX idx_ingested_date (ingested_at)
);
```

Automatically created on first run via Flyway migration V2.

## API Endpoints

### Upload Files
```
POST /api/rag/files
Content-Type: multipart/form-data
Body: files[] (multipart files)
Response: RagUploadResponseDto
```

### List Files
```
GET /api/rag/files
Response: List<RagFileResponseDto>
```

### Delete File
```
DELETE /api/rag/files/{fileId}
Response: 204 No Content
```

### Send Message (Updated)
```
POST /api/chat
Body: { "conversationId": null, "message": "..." }
Response: ChatResponseDto (now includes citedFiles)
```

## Configuration

No new configuration needed! Uses existing settings:
- `assistant.rag.*` - RAG settings (chunk size, batch size, etc.)
- `assistant.qdrant.*` - Qdrant connection
- `langchain4j.ollama.*` - Embedding model
- `langchain4j.ollama.chat-model.*` - Chat model

## Flow Example

```
User: "What are the key points?"
    ↓
[ChatService]
    ├─ Store message in database
    ├─ Search Qdrant: "key points" 
    │  └─ Returns top 5 document chunks
    ├─ Call Ollama with system prompt containing:
    │  "Context from your documents:
    │   ---
    │   Document 1 (document.txt - relevance: 0.92):
    │   [chunk content]
    │   ..."
    ├─ Ollama: "The key points are... [based on document.txt]"
    └─ Return response with citedFiles: ["document.txt"]

Frontend displays:
✓ Assistant message
✓ "Sources: document.txt" badge
```

## Error Handling

| Scenario | Behavior |
|----------|----------|
| No documents uploaded | Chat uses conversation history only |
| Search returns no results | Chat continues normally |
| Qdrant unavailable | Search silently fails, history-only mode |
| Ollama unavailable | Existing error handling (LocalLlamaException) |
| File not found (delete) | HTTP 404 Not Found |
| Large file (10MB+) | Chunked and processed normally |

## Testing Checklist

- [ ] Upload a .txt file
- [ ] Ask a question about the file content
- [ ] Verify answer contains information from the file
- [ ] Check `citedFiles` includes the file name
- [ ] List files with GET /api/rag/files
- [ ] Delete a file with DELETE /api/rag/files/{id}
- [ ] Verify deleted file no longer in listing
- [ ] Chat still works without any uploaded files
- [ ] Web search still works (mix with RAG)
- [ ] Multiple files - chat uses all relevant ones

## Performance Notes

- **Search latency**: ~100-500ms (Qdrant vector search)
- **Embedding gen**: ~50-200ms per query (Ollama) or ~100-300ms (Gemini)
- **Top results**: Limited to 5 for performance
- **Batch size**: 50 segments during ingestion

## Known Limitations

1. **Qdrant cleanup incomplete**: Deleted files remain in Qdrant (workaround: rebuild collection)
2. **PDF support**: Text extraction only (no OCR)
3. **Max file size**: 100MB (configurable)
4. **Max message length**: 12,000 chars
5. **Conversation per day**: One conversation date-based, not custom named

## Next Steps (Phase 2 - Optional)

- [ ] Markdown rendering in chat UI
- [ ] Citation UI - click to see source text
- [ ] File browser with drag-and-drop
- [ ] Settings page for model/temperature
- [ ] Response feedback buttons
- [ ] Conversation export (PDF/Markdown)

## Support

### Troubleshooting

**Q: Upload successful but chat doesn't use the file**
- A: Wait a few seconds for embedding to complete. Check that Qdrant is running.

**Q: Search returns no results**
- A: Your query might be very different from document content. Try related terms.

**Q: "Qdrant connection failed" error**
- A: Ensure Qdrant is running and accessible on configured port (default 6334).

**Q: Files deleted but still appearing in search**
- A: Qdrant vectors remain. You can manually clear Qdrant or rebuild the collection.

**Q: Build fails with compilation errors**
- A: Run `./mvnw clean compile` - should succeed with no errors.

## Build & Deploy

```bash
# Build
./mvnw clean package -DskipTests

# Run (with services)
# Terminal 1: Ollama
ollama serve

# Terminal 2: Qdrant
docker run -p 6333:6333 -p 6334:6334 qdrant/qdrant

# Terminal 3: App
./mvnw spring-boot:run
# or
java -jar target/mypersonalassist-0.0.1-SNAPSHOT.jar
```

**Ready to use!** Open http://localhost:8080/chat and start uploading files.

---

**Implementation Date**: August 30, 2026
**Last Updated**: Phase 1 Complete ✅
