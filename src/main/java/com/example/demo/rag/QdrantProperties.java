package com.example.demo.rag;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "assistant.qdrant")
public record QdrantProperties(
		String host,
		Integer grpcPort,
		Integer httpPort,
		String collection,
		Integer embeddingDimensions) {

	public QdrantProperties {
		if (host == null || host.isBlank()) {
			host = "localhost";
		}
		if (grpcPort == null) {
			grpcPort = 6334;
		}
		if (httpPort == null) {
			httpPort = 6333;
		}
		if (collection == null || collection.isBlank()) {
			collection = "personal_assist_rag";
		}
		if (embeddingDimensions == null || embeddingDimensions < 1) {
			embeddingDimensions = 768;
		}
	}
}
