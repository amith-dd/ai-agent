package com.example.demo.rag;

import java.time.Instant;

/**
 * DTO for returning file information to the client.
 * Used by the file listing endpoint.
 */
public record RagFileResponseDto(
		Long id,
		String fileName,
		Long fileSize,
		String contentType,
		Integer chunksCount,
		Instant ingestedAt) {
}
