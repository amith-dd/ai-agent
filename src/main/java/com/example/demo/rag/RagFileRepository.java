package com.example.demo.rag;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for RAG file metadata.
 */
@Repository
public interface RagFileRepository extends JpaRepository<RagFileEntity, Long> {

	/**
	 * Find a file by its file name.
	 */
	Optional<RagFileEntity> findByFileName(String fileName);

	/**
	 * Find a file by its file path.
	 */
	Optional<RagFileEntity> findByFilePath(String filePath);

	/**
	 * List all files ordered by ingested date (most recent first).
	 */
	List<RagFileEntity> findAllByOrderByIngestedAtDesc();
}
