package com.example.demo.chat;

import com.example.demo.rag.RetrievedDocument;
import com.example.demo.rag.RagQueryService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

	private final ChatConversationService conversationService;
	private final LocalLlamaClient localLlamaClient;
	private final RagQueryService ragQueryService;

	public ChatService(
			ChatConversationService conversationService,
			LocalLlamaClient localLlamaClient,
			RagQueryService ragQueryService) {
		this.conversationService = conversationService;
		this.localLlamaClient = localLlamaClient;
		this.ragQueryService = ragQueryService;
	}

	public ChatResponseDto sendMessage(ChatRequestDto request) {
		ChatTurnContext turnContext = conversationService.appendUserMessage(
				request.conversationId(),
				request.message());

		// Search for relevant documents from uploaded files
		List<RetrievedDocument> ragDocuments = ragQueryService.searchDocuments(
				request.message(),
				5); // Top 5 most relevant documents

		// Generate response with RAG context
		LlamaReply reply = localLlamaClient.generateReply(
				turnContext.recentMessages(),
				ragDocuments);

		ChatMessageDto assistantMessage = conversationService.appendAssistantMessage(
				turnContext.conversation().id(),
				reply.content(),
				reply.modelName());

		List<ConversationSummaryDto> conversations = conversationService.listConversations();
		ConversationSummaryDto activeConversation = conversations.stream()
				.filter(conversation -> conversation.id().equals(turnContext.conversation().id()))
				.findFirst()
				.orElse(turnContext.conversation());

		// Extract file names used for citation
		List<String> citedFiles = ragQueryService.extractFileNames(ragDocuments);

		return new ChatResponseDto(
				activeConversation,
				turnContext.userMessage(),
				assistantMessage,
				conversations,
				citedFiles);
	}
}
