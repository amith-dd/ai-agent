package com.example.demo.chat;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "langchain4j.ollama.chat-model")
public record LocalLlamaProperties(
		String baseUrl,
		String modelName,
		Double temperature,
		Duration timeout) {

	public LocalLlamaProperties {
		if (baseUrl == null || baseUrl.isBlank()) {
			baseUrl = "http://localhost:11434";
		}
		if (modelName == null || modelName.isBlank()) {
			modelName = "llama3.1";
		}
		if (temperature == null) {
			temperature = 0.2;
		}
		if (timeout == null) {
			timeout = Duration.ofSeconds(120);
		}
	}
}
