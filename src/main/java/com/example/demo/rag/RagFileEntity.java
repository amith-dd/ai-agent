package com.example.demo.rag;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Entity representing an uploaded file tracked in the RAG system.
 * Stores metadata about files and their embeddings in Qdrant.
 */
@Entity
@Table(name = "rag_files", indexes = {
		@Index(name = "idx_ingested_date", columnList = "ingested_at")
})
public class RagFileEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 255)
	private String fileName;

	@Column(nullable = false, length = 512, unique = true)
	private String filePath;

	@Column(nullable = false)
	private Long fileSize;

	@Column(length = 100)
	private String contentType;

	@Column(nullable = false)
	private Integer chunksCount;

	@Column(nullable = false)
	private Instant ingestedAt;

	// Constructors
	public RagFileEntity() {
	}

	public RagFileEntity(String fileName, String filePath, Long fileSize, String contentType, Integer chunksCount) {
		this.fileName = fileName;
		this.filePath = filePath;
		this.fileSize = fileSize;
		this.contentType = contentType;
		this.chunksCount = chunksCount;
		this.ingestedAt = Instant.now();
	}

	// Getters and Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public Long getFileSize() {
		return fileSize;
	}

	public void setFileSize(Long fileSize) {
		this.fileSize = fileSize;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public Integer getChunksCount() {
		return chunksCount;
	}

	public void setChunksCount(Integer chunksCount) {
		this.chunksCount = chunksCount;
	}

	public Instant getIngestedAt() {
		return ingestedAt;
	}

	public void setIngestedAt(Instant ingestedAt) {
		this.ingestedAt = ingestedAt;
	}

	@Override
	public String toString() {
		return "RagFileEntity{" +
				"id=" + id +
				", fileName='" + fileName + '\'' +
				", fileSize=" + fileSize +
				", chunksCount=" + chunksCount +
				", ingestedAt=" + ingestedAt +
				'}';
	}
}
