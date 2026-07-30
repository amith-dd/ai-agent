package com.example.demo.chat;

public class ConversationNotFoundException extends RuntimeException {

	public ConversationNotFoundException(Long conversationId) {
		super("Conversation was not found: " + conversationId);
	}
}
