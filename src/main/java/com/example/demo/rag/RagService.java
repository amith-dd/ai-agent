package com.example.demo.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.env.Environment;

@Service
public class RagService {

	private static final String STATUS_STORED = "stored";
	private static final String STATUS_FAILED = "failed";

	private final RagProperties ragProperties;
	private final QdrantProperties qdrantProperties;
	private final OllamaEmbeddingProperties ollamaProperties;
    private final GeminiEmbeddingProperties geminiProperties;
	private final HttpClient httpClient;
    private final Environment env;
	private final RagFileRepository ragFileRepository;

	public RagService(
			RagProperties ragProperties,
			QdrantProperties qdrantProperties,
			OllamaEmbeddingProperties ollamaProperties,
            GeminiEmbeddingProperties geminiProperties,
            Environment env,
			RagFileRepository ragFileRepository) {
		this.ragProperties = ragProperties;
		this.qdrantProperties = qdrantProperties;
		this.ollamaProperties = ollamaProperties;
        this.geminiProperties = geminiProperties;
        this.env = env;
		this.ragFileRepository = ragFileRepository;
		this.httpClient = HttpClient.newHttpClient();
	}

	public RagUploadResponseDto ingestFiles(List<MultipartFile> files) {
		if (files == null || files.isEmpty()) {
			return new RagUploadResponseDto(List.of());
		}

		List<RagFileStatusDto> statuses = new ArrayList<>();
		for (MultipartFile file : files) {
			statuses.add(ingestFile(file));
		}
		return new RagUploadResponseDto(statuses);
	}

	private RagFileStatusDto ingestFile(MultipartFile file) {
		String fileName = cleanFileName(file.getOriginalFilename());
		long size = file.getSize();
		String contentType = file.getContentType();
		try {
			validateTextFile(file, fileName);
			Path storedFile = saveFile(file, fileName);
			String text = Files.readString(storedFile, StandardCharsets.UTF_8);
			if (!StringUtils.hasText(text)) {
				return new RagFileStatusDto(fileName, STATUS_FAILED, "File does not contain text.", size, 0);
			}

			ensureCollectionExists();
			int chunksStored = storeText(fileName, storedFile, text, contentType);
			return new RagFileStatusDto(
					fileName,
					STATUS_STORED,
					"Stored in vector database.",
					size,
					chunksStored);
		}
		catch (Exception ex) {
			return new RagFileStatusDto(fileName, STATUS_FAILED, userMessage(ex), size, 0);
		}
	}

	private int storeText(String fileName, Path storedFile, String text, String contentType) {
		Metadata metadata = Metadata.from(Document.FILE_NAME, fileName)
				.put(Document.ABSOLUTE_DIRECTORY_PATH, storedFile.getParent().toAbsolutePath().toString())
				.put("source_path", storedFile.toAbsolutePath().toString())
				.put("ingested_at", Instant.now().toString());
		Document document = Document.from(text, metadata);
		List<TextSegment> segments = DocumentSplitters
				.recursive(ragProperties.chunkSize(), ragProperties.chunkOverlap())
				.split(document);
		if (segments.isEmpty()) {
			return 0;
		}

		EmbeddingModel embeddingModel = embeddingModel();
		EmbeddingStore<TextSegment> embeddingStore = embeddingStore();
		for (int index = 0; index < segments.size(); index += ragProperties.embeddingBatchSize()) {
			int end = Math.min(index + ragProperties.embeddingBatchSize(), segments.size());
			List<TextSegment> batch = segments.subList(index, end);
			List<Embedding> embeddings = embeddingModel.embedAll(batch).content();
			List<String> ids = batch.stream()
					.map(segment -> UUID.randomUUID().toString())
					.toList();
			embeddingStore.addAll(ids, embeddings, batch);
			pauseBetweenBatches(index, segments.size());
		}

		// Save file metadata to database
		int chunksCount = segments.size();
		RagFileEntity fileEntity = new RagFileEntity(
				fileName,
				storedFile.toAbsolutePath().toString(),
				storedFile.toFile().length(),
				contentType,
				chunksCount);
		ragFileRepository.save(fileEntity);

		return chunksCount;
	}

	private EmbeddingModel embeddingModel() {
        String provider = env.getProperty("assistant.llm.cloud.provider", "gemini");
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
				//.dimensions(qdrantProperties.embeddingDimensions()) // Removed because of compatibility issues across langchain4j ollama versions
				.build();
	}

	private EmbeddingStore<TextSegment> embeddingStore() {
		return QdrantEmbeddingStore.builder()
				.host(qdrantProperties.host())
				.port(qdrantProperties.grpcPort())
				.collectionName(qdrantProperties.collection())
				.payloadTextKey("text")
				.build();
	}

	private void ensureCollectionExists() throws IOException, InterruptedException {
		URI collectionUri = URI.create("http://%s:%d/collections/%s".formatted(
				qdrantProperties.host(),
				qdrantProperties.httpPort(),
				qdrantProperties.collection()));
		HttpResponse<String> getResponse = httpClient.send(
				HttpRequest.newBuilder(collectionUri).GET().build(),
				HttpResponse.BodyHandlers.ofString());
		if (getResponse.statusCode() == 200) {
			return;
		}
		if (getResponse.statusCode() != 404) {
			throw new IllegalStateException("Qdrant collection check failed with HTTP " + getResponse.statusCode() + ".");
		}

		String body = """
				{"vectors":{"size":%d,"distance":"Cosine"}}
				""".formatted(qdrantProperties.embeddingDimensions());
		HttpResponse<String> putResponse = httpClient.send(
				HttpRequest.newBuilder(collectionUri)
						.header("Content-Type", "application/json")
						.PUT(HttpRequest.BodyPublishers.ofString(body))
						.build(),
				HttpResponse.BodyHandlers.ofString());
		if (putResponse.statusCode() < 200 || putResponse.statusCode() >= 300) {
			throw new IllegalStateException("Qdrant collection creation failed with HTTP " + putResponse.statusCode() + ".");
		}
	}

	private Path saveFile(MultipartFile file, String fileName) throws IOException {
		Files.createDirectories(ragProperties.uploadDir());
		Path target = ragProperties.uploadDir()
				.resolve(UUID.randomUUID() + "-" + fileName)
				.normalize();
		if (!target.startsWith(ragProperties.uploadDir().normalize())) {
			throw new IllegalArgumentException("Invalid file path.");
		}
		try (InputStream inputStream = file.getInputStream()) {
			Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
		}
		return target;
	}

	private void validateTextFile(MultipartFile file, String fileName) {
		if (file.isEmpty()) {
			throw new IllegalArgumentException("File is empty.");
		}
		if (!isTextFile(file, fileName)) {
			throw new IllegalArgumentException("Only .txt and .md text files are supported for indexing.");
		}
	}

	private boolean isTextFile(MultipartFile file, String fileName) {
		String lowerName = fileName.toLowerCase(Locale.ROOT);
		String contentType = file.getContentType();
		boolean textExtension = lowerName.endsWith(".txt") || lowerName.endsWith(".md") || lowerName.endsWith(".markdown");
		boolean textContentType = contentType == null
				|| contentType.startsWith("text/")
				|| ragProperties.allowedContentTypes().contains(contentType)
				|| "application/octet-stream".equals(contentType);
		return textExtension && textContentType;
	}

	private String cleanFileName(String originalFileName) {
		String cleaned = StringUtils.cleanPath(originalFileName == null ? "uploaded-file.txt" : originalFileName);
		if (!StringUtils.hasText(cleaned) || cleaned.contains("..")) {
			return "uploaded-file.txt";
		}
		return Path.of(cleaned).getFileName().toString();
	}

	private void pauseBetweenBatches(int index, int totalSegments) {
		if (index + ragProperties.embeddingBatchSize() >= totalSegments || ragProperties.embeddingBatchDelay().isZero()) {
			return;
		}
		try {
			Thread.sleep(ragProperties.embeddingBatchDelay().toMillis());
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	private String userMessage(Exception ex) {
		if (ex instanceof IllegalArgumentException) {
			return ex.getMessage();
		}
		return "Could not store in vector database. Check Ollama embeddings and Qdrant are running.";
	}

	/**
	 * Delete a file from RAG storage.
	 * Removes from database, disk, and Qdrant vector store.
	 */
	public void deleteFile(RagFileEntity file) {
		// Delete from disk
		try {
			Path filePath = Paths.get(file.getFilePath());
			if (Files.exists(filePath)) {
				Files.delete(filePath);
			}
		}
		catch (IOException ex) {
			System.err.println("Warning: Could not delete file from disk: " + file.getFilePath());
		}

		// Note: Deleting from Qdrant requires deleting by segment IDs, which we don't currently track.
		// This would require storing segment IDs in the database. For now, segments remain in Qdrant
		// but won't match semantic searches for deleted files since they won't have associated metadata.
		// TODO: Implement segment ID tracking for complete Qdrant cleanup.
	}
}
