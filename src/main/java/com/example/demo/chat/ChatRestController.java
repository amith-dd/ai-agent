package com.example.demo.chat;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ChatRestController {

	private final ChatService chatService;
	private final ChatConversationService conversationService;

	public ChatRestController(ChatService chatService, ChatConversationService conversationService) {
		this.chatService = chatService;
		this.conversationService = conversationService;
	}

	@GetMapping("/conversations")
	public List<ConversationSummaryDto> conversations() {
		return conversationService.listConversations();
	}

	@GetMapping("/conversations/{conversationId}/messages")
	public List<ChatMessageDto> messages(@PathVariable Long conversationId) {
		return conversationService.listMessages(conversationId);
	}

	@PostMapping("/chat")
	public ChatResponseDto chat(@Valid @RequestBody ChatRequestDto request) {
		return chatService.sendMessage(request);
	}
}
