package com.example.demo.chat;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.ollama.OllamaChatModel;
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

	public LocalLlamaClient(LocalLlamaProperties properties) {
		this.configuredModelName = properties.modelName();
		this.chatModel = OllamaChatModel.builder()
				.httpClientBuilder(new JdkHttpClientBuilder())
				.baseUrl(properties.baseUrl())
				.modelName(properties.modelName())
				.temperature(properties.temperature())
				.timeout(properties.timeout())
				.build();
	}

	public LlamaReply generateReply(List<StoredChatMessage> recentMessages) {
		try {
			ChatResponse response = chatModel.chat(toLangChainMessages(recentMessages));
			String content = response.aiMessage() == null ? "" : response.aiMessage().text();
			if (content == null || content.isBlank()) {
				content = "I did not receive a response from the local Llama model.";
			}
			String modelName = response.modelName() == null ? configuredModelName : response.modelName();
			return new LlamaReply(content.trim(), modelName);
		}
		catch (RuntimeException ex) {
			throw new LocalLlamaException(
					"Local Llama is not reachable. Start Ollama and make sure the configured model is pulled.",
					ex);
		}
	}

	private List<ChatMessage> toLangChainMessages(List<StoredChatMessage> storedMessages) {
		List<ChatMessage> messages = new ArrayList<>();
		messages.add(SystemMessage.from(SYSTEM_PROMPT));
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
}
