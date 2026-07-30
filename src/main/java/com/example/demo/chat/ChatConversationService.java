package com.example.demo.chat;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatConversationService {

	private static final int CONTEXT_MESSAGE_LIMIT = 18;

	private final ChatConversationRepository conversationRepository;
	private final ConversationMessageRepository messageRepository;
	private final ZoneId zoneId = ZoneId.systemDefault();

	public ChatConversationService(
			ChatConversationRepository conversationRepository,
			ConversationMessageRepository messageRepository) {
		this.conversationRepository = conversationRepository;
		this.messageRepository = messageRepository;
	}

	@Transactional
	public ChatTurnContext appendUserMessage(Long conversationId, String rawContent) {
		String content = normalize(rawContent);
		ChatConversation conversation = getOrCreateConversation(conversationId);
		if (messageRepository.countByConversation_Id(conversation.getId()) == 0) {
			conversation.setTitle(buildTitle(content));
		}
		conversation.touch();

		ConversationMessage userMessage = messageRepository.save(
				new ConversationMessage(conversation, MessageRole.USER, content, null));

		List<StoredChatMessage> recentMessages = recentStoredMessages(conversation.getId());
		return new ChatTurnContext(toSummary(conversation), toDto(userMessage), recentMessages);
	}

	@Transactional
	public ChatMessageDto appendAssistantMessage(Long conversationId, String content, String modelName) {
		ChatConversation conversation = conversationRepository.findById(conversationId)
				.orElseThrow(() -> new ConversationNotFoundException(conversationId));
		conversation.touch();
		ConversationMessage assistantMessage = messageRepository.save(
				new ConversationMessage(conversation, MessageRole.ASSISTANT, normalize(content), modelName));
		return toDto(assistantMessage);
	}

	@Transactional(readOnly = true)
	public List<ConversationSummaryDto> listConversations() {
		return conversationRepository.findAllByOrderByConversationDateDesc()
				.stream()
				.map(this::toSummary)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<ChatMessageDto> listMessages(Long conversationId) {
		if (!conversationRepository.existsById(conversationId)) {
			throw new ConversationNotFoundException(conversationId);
		}
		return messageRepository.findAllForConversation(conversationId)
				.stream()
				.map(this::toDto)
				.toList();
	}

	private ChatConversation getOrCreateConversation(Long conversationId) {
		if (conversationId != null) {
			return conversationRepository.findById(conversationId)
					.orElseThrow(() -> new ConversationNotFoundException(conversationId));
		}
		LocalDate today = LocalDate.now(zoneId);
		return conversationRepository.findByConversationDate(today)
				.orElseGet(() -> conversationRepository.save(new ChatConversation(today, "Conversation " + today)));
	}

	private List<StoredChatMessage> recentStoredMessages(Long conversationId) {
		List<ConversationMessage> recent = new ArrayList<>(messageRepository.findRecentForConversation(
				conversationId,
				PageRequest.of(0, CONTEXT_MESSAGE_LIMIT)));
		Collections.reverse(recent);
		return recent.stream()
				.map(message -> new StoredChatMessage(message.getRole(), message.getContent()))
				.toList();
	}

	private ConversationSummaryDto toSummary(ChatConversation conversation) {
		long messageCount = conversation.getId() == null
				? 0
				: messageRepository.countByConversation_Id(conversation.getId());
		return new ConversationSummaryDto(
				conversation.getId(),
				conversation.getConversationDate(),
				conversation.getTitle(),
				conversation.getUpdatedAt(),
				messageCount);
	}

	private ChatMessageDto toDto(ConversationMessage message) {
		return new ChatMessageDto(
				message.getId(),
				message.getRole().name().toLowerCase(),
				message.getContent(),
				message.getModelName(),
				message.getCreatedAt());
	}

	private String normalize(String content) {
		return content == null ? "" : content.trim();
	}

	private String buildTitle(String content) {
		String firstLine = content.lines().findFirst().orElse("Conversation").trim();
		if (firstLine.length() <= 72) {
			return firstLine.isBlank() ? "Conversation" : firstLine;
		}
		return firstLine.substring(0, 72).trim() + "...";
	}
}
