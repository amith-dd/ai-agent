package com.example.demo.rag;

import java.util.Map;

/**
 * DTO representing a document chunk retrieved from Qdrant vector store.
 * Used to pass context from RAG search to the chat service and LLM.
 */
public record RetrievedDocument(
		String id,
		String fileName,
		String content,
		double score,
		Map<String, String> metadata) {

	@Override
	public String toString() {
		return "RetrievedDocument{" +
				"fileName='" + fileName + '\'' +
				", score=" + score +
				", contentLength=" + (content != null ? content.length() : 0) +
				'}';
	}
}
