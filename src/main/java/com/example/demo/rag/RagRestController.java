package com.example.demo.rag;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/rag")
public class RagRestController {

	private final RagService ragService;
	private final RagFileRepository ragFileRepository;

	public RagRestController(RagService ragService, RagFileRepository ragFileRepository) {
		this.ragService = ragService;
		this.ragFileRepository = ragFileRepository;
	}

	/**
	 * Upload files for RAG indexing.
	 */
	@PostMapping(path = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public RagUploadResponseDto uploadFiles(@RequestParam("files") List<MultipartFile> files) {
		return ragService.ingestFiles(files);
	}

	/**
	 * List all uploaded files with metadata.
	 */
	@GetMapping("/files")
	public List<RagFileResponseDto> listFiles() {
		return ragFileRepository.findAllByOrderByIngestedAtDesc()
				.stream()
				.map(this::toResponseDto)
				.toList();
	}

	/**
	 * Delete a file from RAG storage.
	 * Removes the file from database, disk, and Qdrant vector store.
	 */
	@DeleteMapping("/files/{fileId}")
	public void deleteFile(@PathVariable Long fileId) {
		RagFileEntity file = ragFileRepository.findById(fileId)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND,
						"File not found with id: " + fileId));

		ragService.deleteFile(file);

		ragFileRepository.delete(file);
	}

	private RagFileResponseDto toResponseDto(RagFileEntity entity) {
		return new RagFileResponseDto(
				entity.getId(),
				entity.getFileName(),
				entity.getFileSize(),
				entity.getContentType(),
				entity.getChunksCount(),
				entity.getIngestedAt());
	}
}
