package com.example.demo.chat;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

	private final ChatConversationService conversationService;
	private final LocalLlamaClient localLlamaClient;

	public ChatService(ChatConversationService conversationService, LocalLlamaClient localLlamaClient) {
		this.conversationService = conversationService;
		this.localLlamaClient = localLlamaClient;
	}

	public ChatResponseDto sendMessage(ChatRequestDto request) {
		ChatTurnContext turnContext = conversationService.appendUserMessage(
				request.conversationId(),
				request.message());
		LlamaReply reply = localLlamaClient.generateReply(turnContext.recentMessages());
		ChatMessageDto assistantMessage = conversationService.appendAssistantMessage(
				turnContext.conversation().id(),
				reply.content(),
				reply.modelName());
		List<ConversationSummaryDto> conversations = conversationService.listConversations();
		ConversationSummaryDto activeConversation = conversations.stream()
				.filter(conversation -> conversation.id().equals(turnContext.conversation().id()))
				.findFirst()
				.orElse(turnContext.conversation());
		return new ChatResponseDto(
				activeConversation,
				turnContext.userMessage(),
				assistantMessage,
				conversations);
	}
}
