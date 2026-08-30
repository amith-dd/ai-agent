package com.example.demo.chat;

import java.util.List;

public record ChatResponseDto(
		ConversationSummaryDto conversation,
		ChatMessageDto userMessage,
		ChatMessageDto assistantMessage,
		List<ConversationSummaryDto> conversations,
		List<String> citedFiles) {
}
