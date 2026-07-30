package com.example.demo.rag;

public record RagFileStatusDto(
		String fileName,
		String status,
		String message,
		long sizeBytes,
		int chunksStored) {
}
