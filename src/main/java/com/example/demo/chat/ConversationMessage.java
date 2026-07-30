package com.example.demo.chat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "chat_messages")
public class ConversationMessage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "conversation_id", nullable = false)
	private ChatConversation conversation;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private MessageRole role;

	@Lob
	@Column(nullable = false, columnDefinition = "LONGTEXT")
	private String content;

	@Column(name = "model_name", length = 120)
	private String modelName;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected ConversationMessage() {
	}

	public ConversationMessage(ChatConversation conversation, MessageRole role, String content, String modelName) {
		this.conversation = conversation;
		this.role = role;
		this.content = content;
		this.modelName = modelName;
	}

	@PrePersist
	void onCreate() {
		this.createdAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public ChatConversation getConversation() {
		return conversation;
	}

	public MessageRole getRole() {
		return role;
	}

	public String getContent() {
		return content;
	}

	public String getModelName() {
		return modelName;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
