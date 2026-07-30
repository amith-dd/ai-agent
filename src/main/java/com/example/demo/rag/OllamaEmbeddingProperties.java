package com.example.demo.rag;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "langchain4j.ollama.embedding-model")
public record OllamaEmbeddingProperties(
		String baseUrl,
		String modelName,
		Duration timeout) {

	public OllamaEmbeddingProperties {
		if (baseUrl == null || baseUrl.isBlank()) {
			baseUrl = "http://localhost:11434";
		}
		if (modelName == null || modelName.isBlank()) {
			modelName = "nomic-embed-text";
		}
		if (timeout == null) {
			timeout = Duration.ofSeconds(120);
		}
	}
}
