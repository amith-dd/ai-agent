package com.example.demo.chat;

import java.util.List;

public record ChatTurnContext(
		ConversationSummaryDto conversation,
		ChatMessageDto userMessage,
		List<StoredChatMessage> recentMessages) {
}
