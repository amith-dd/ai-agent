package com.example.demo.rag;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "assistant.rag")
public record RagProperties(
		Path uploadDir,
		List<String> allowedContentTypes,
		Integer chunkSize,
		Integer chunkOverlap,
		Integer embeddingBatchSize,
		Duration embeddingBatchDelay,
		Integer maxParallelFiles) {

	public RagProperties {
		if (uploadDir == null) {
			uploadDir = Path.of(System.getProperty("user.home"), ".mypersonalassist", "uploads");
		}
		if (allowedContentTypes == null || allowedContentTypes.isEmpty()) {
			allowedContentTypes = List.of("text/plain", "text/markdown");
		}
		if (chunkSize == null || chunkSize < 1) {
			chunkSize = 800;
		}
		if (chunkOverlap == null || chunkOverlap < 0) {
			chunkOverlap = 120;
		}
		if (embeddingBatchSize == null || embeddingBatchSize < 1) {
			embeddingBatchSize = 50;
		}
		if (embeddingBatchDelay == null) {
			embeddingBatchDelay = Duration.ofSeconds(2);
		}
		if (maxParallelFiles == null || maxParallelFiles < 1) {
			maxParallelFiles = 2;
		}
	}
}
