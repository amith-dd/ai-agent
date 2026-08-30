package com.example.demo.chat;

import com.example.demo.rag.RetrievedDocument;
import com.example.demo.rag.RagQueryService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class LocalLlamaClient {

	private static final String SYSTEM_PROMPT = """
			You are a local personal assistant running on the user's machine.
			Answer clearly and practically. Keep private information local.
			When a requested desktop, voice, or RAG tool is not connected yet, say that the feature is planned and answer what you can.
			""";

	private final ChatModel chatModel;
	private final String configuredModelName;
	private final Assistant assistant;
	private final RagQueryService ragQueryService;

	interface Assistant {
		Result<String> chat(List<ChatMessage> messages);
	}

	public LocalLlamaClient(
			LocalLlamaProperties properties,
			WebSearchTool webSearchTool,
			RagQueryService ragQueryService) {
		this.configuredModelName = properties.modelName();
		this.ragQueryService = ragQueryService;
		this.chatModel = OllamaChatModel.builder()
				.httpClientBuilder(new JdkHttpClientBuilder())
				.baseUrl(properties.baseUrl())
				.modelName(properties.modelName())
				.temperature(properties.temperature())
				.timeout(properties.timeout())
				.build();
		this.assistant = AiServices.builder(Assistant.class)
				.chatModel(this.chatModel)
				.tools(webSearchTool)
				.build();
	}

	/**
	 * Generate a reply from the LLM with optional RAG context.
	 * 
	 * @param recentMessages recent conversation messages
	 * @param ragDocuments retrieved documents for context (can be empty list)
	 * @return LlamaReply with content and model name
	 */
	public LlamaReply generateReply(List<StoredChatMessage> recentMessages, List<RetrievedDocument> ragDocuments) {
		try {
			Result<String> response = assistant.chat(toLangChainMessages(recentMessages, ragDocuments));
			String content = response.content();
			if (content == null || content.isBlank()) {
				content = "I did not receive a response from the local Llama model.";
			}
			String modelName = configuredModelName;
			return new LlamaReply(content.trim(), modelName);
		}
		catch (RuntimeException ex) {
			throw new LocalLlamaException(
					"Local Llama is not reachable. Start Ollama and make sure the configured model is pulled.",
					ex);
		}
	}

	/**
	 * Generate a reply from the LLM without RAG context (backward compatibility).
	 * 
	 * @param recentMessages recent conversation messages
	 * @return LlamaReply with content and model name
	 */
	public LlamaReply generateReply(List<StoredChatMessage> recentMessages) {
		return generateReply(recentMessages, new ArrayList<>());
	}

	private List<ChatMessage> toLangChainMessages(
			List<StoredChatMessage> storedMessages,
			List<RetrievedDocument> ragDocuments) {
		List<ChatMessage> messages = new ArrayList<>();

		// Build augmented system prompt with RAG context
		String systemPrompt = buildSystemPrompt(ragDocuments);
		messages.add(SystemMessage.from(systemPrompt));

		// Add conversation history
		for (StoredChatMessage storedMessage : storedMessages) {
			if (storedMessage.role() == MessageRole.USER) {
				messages.add(UserMessage.from(storedMessage.content()));
			}
			else {
				messages.add(AiMessage.from(storedMessage.content()));
			}
		}
		return messages;
	}

	private String buildSystemPrompt(List<RetrievedDocument> ragDocuments) {
		String basePrompt = SYSTEM_PROMPT;

		// Augment system prompt with RAG context if documents are available
		if (ragDocuments != null && !ragDocuments.isEmpty()) {
			String ragContext = ragQueryService.formatDocumentsForPrompt(ragDocuments);
			basePrompt = basePrompt + ragContext;
		}

		return basePrompt;
	}
}
