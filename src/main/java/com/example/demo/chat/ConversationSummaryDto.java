package com.example.demo.chat;

import java.time.Instant;
import java.time.LocalDate;

public record ConversationSummaryDto(
		Long id,
		LocalDate date,
		String title,
		Instant updatedAt,
		long messageCount) {
}
