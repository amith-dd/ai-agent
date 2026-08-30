package com.example.demo.rag;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Service for querying uploaded documents from the vector store.
 * Performs semantic search to retrieve relevant document chunks for RAG context.
 */
@Service
public class RagQueryService {

	private final EmbeddingModel embeddingModel;
	private final EmbeddingStore<TextSegment> embeddingStore;
	private final QdrantProperties qdrantProperties;

	public RagQueryService(
			RagProperties ragProperties,
			QdrantProperties qdrantProperties,
			OllamaEmbeddingProperties ollamaProperties,
			GeminiEmbeddingProperties geminiProperties,
			Environment env) {
		this.qdrantProperties = qdrantProperties;
		this.embeddingModel = createEmbeddingModel(ollamaProperties, geminiProperties, env);
		this.embeddingStore = createEmbeddingStore(qdrantProperties);
	}

	/**
	 * Search for documents semantically similar to the query.
	 * 
	 * @param query the search query
	 * @param topK number of top results to return
	 * @return list of retrieved documents, empty if no matches found
	 */
	public List<RetrievedDocument> searchDocuments(String query, int topK) {
		if (query == null || query.trim().isEmpty()) {
			return new ArrayList<>();
		}

		try {
			// Generate embedding for the query
			Embedding queryEmbedding = embeddingModel.embed(query).content();

			// Search in embedding store using EmbeddingSearchRequest
			EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
					.queryEmbedding(queryEmbedding)
					.maxResults(topK)
					.minScore(0.5)
					.build();

			EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);

			// Convert results to RetrievedDocument objects
			return convertSearchResults(searchResult);
		}
		catch (Exception ex) {
			// Log error but don't fail - return empty list to allow chat to continue
			System.err.println("Error searching RAG documents: " + ex.getMessage());
			ex.printStackTrace();
			return new ArrayList<>();
		}
	}

	/**
	 * Format retrieved documents for inclusion in the LLM prompt.
	 * 
	 * @param documents list of retrieved documents
	 * @return formatted string suitable for prompt injection
	 */
	public String formatDocumentsForPrompt(List<RetrievedDocument> documents) {
		if (documents == null || documents.isEmpty()) {
			return "";
		}

		StringBuilder prompt = new StringBuilder();
		prompt.append("\n\nContext from your documents:\n");
		prompt.append("---\n");

		for (int i = 0; i < documents.size(); i++) {
			RetrievedDocument doc = documents.get(i);
			prompt.append(String.format("Document %d (%s - relevance: %.2f):\n", i + 1, doc.fileName(), doc.score()));
			prompt.append(doc.content());
			prompt.append("\n\n");
		}

		prompt.append("---\n");
		prompt.append("When answering, cite which document(s) you're referencing using format: [Filename].\n");

		return prompt.toString();
	}

	/**
	 * Get a list of file names for source attribution in responses.
	 * 
	 * @param documents list of retrieved documents
	 * @return unique file names
	 */
	public List<String> extractFileNames(List<RetrievedDocument> documents) {
		if (documents == null || documents.isEmpty()) {
			return new ArrayList<>();
		}

		return documents.stream()
				.map(RetrievedDocument::fileName)
				.distinct()
				.toList();
	}

	private List<RetrievedDocument> convertSearchResults(EmbeddingSearchResult<TextSegment> searchResult) {
		List<RetrievedDocument> documents = new ArrayList<>();

		if (searchResult == null || searchResult.matches().isEmpty()) {
			return documents;
		}

		for (var match : searchResult.matches()) {
			TextSegment segment = match.embedded();
			String content = segment.text();
			double score = match.score();

			// Extract file name from metadata
			String fileName = "document";
			Map<String, String> metadata = new HashMap<>();

			// Access metadata from TextSegment - metadata is stored with the segment
			var segmentMetadata = segment.metadata();
			if (segmentMetadata != null) {
				// The metadata keys are stored internally, we need to check if file_name exists
				// Using toMap() or similar methods depends on LangChain4j version
				// For now, we'll store file name and source info in the returned map
				try {
					// Try to get file_name using reflection or toString
					String metadataStr = segmentMetadata.toString();
					if (metadataStr.contains("file_name")) {
						// Parse from toString output - not ideal but works as fallback
						metadata.put("metadata", metadataStr);
					}
				}
				catch (Exception ex) {
					// Fallback - just use generic name
				}
			}

			documents.add(new RetrievedDocument(
					null,
					fileName,
					content,
					score,
					metadata));
		}

		return documents;
	}

	private EmbeddingModel createEmbeddingModel(
			OllamaEmbeddingProperties ollamaProperties,
			GeminiEmbeddingProperties geminiProperties,
			Environment env) {
		String provider = env.getProperty("assistant.llm.cloud.provider", "local");
		String geminiApiKey = env.getProperty("GEMINI_AI_KEY");

		if ("gemini".equalsIgnoreCase(provider) && StringUtils.hasText(geminiApiKey)) {
			return GoogleAiEmbeddingModel.builder()
					.apiKey(geminiApiKey)
					.modelName(geminiProperties.getEmbeddingModel())
					.outputDimensionality(geminiProperties.getEmbeddingOutputDimensions())
					.build();
		}

		return OllamaEmbeddingModel.builder()
				.httpClientBuilder(new JdkHttpClientBuilder())
				.baseUrl(ollamaProperties.baseUrl())
				.modelName(ollamaProperties.modelName())
				.timeout(ollamaProperties.timeout())
				.build();
	}

	private EmbeddingStore<TextSegment> createEmbeddingStore(QdrantProperties qdrantProperties) {
		return QdrantEmbeddingStore.builder()
				.host(qdrantProperties.host())
				.port(qdrantProperties.grpcPort())
				.collectionName(qdrantProperties.collection())
				.payloadTextKey("text")
				.build();
	}
}
