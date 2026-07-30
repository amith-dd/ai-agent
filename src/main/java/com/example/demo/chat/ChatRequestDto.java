package com.example.demo.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequestDto(
		Long conversationId,
		@NotBlank(message = "Message is required")
		@Size(max = 12000, message = "Message is too long")
		String message) {
}
