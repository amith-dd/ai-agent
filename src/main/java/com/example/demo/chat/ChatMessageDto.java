package com.example.demo.chat;

import java.time.Instant;

public record ChatMessageDto(
		Long id,
		String role,
		String content,
		String modelName,
		Instant createdAt) {
}
