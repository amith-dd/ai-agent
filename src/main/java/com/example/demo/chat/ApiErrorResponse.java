package com.example.demo.chat;

import java.time.Instant;

public record ApiErrorResponse(String message, Instant timestamp) {

	public static ApiErrorResponse now(String message) {
		return new ApiErrorResponse(message, Instant.now());
	}
}
