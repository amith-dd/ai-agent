package com.example.demo.chat;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ChatExceptionHandler {

	@ExceptionHandler(LocalLlamaException.class)
	public ResponseEntity<ApiErrorResponse> handleLocalLlamaException(LocalLlamaException ex) {
		return ResponseEntity
				.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(ApiErrorResponse.now(ex.getMessage()));
	}

	@ExceptionHandler(ConversationNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleConversationNotFound(ConversationNotFoundException ex) {
		return ResponseEntity
				.status(HttpStatus.NOT_FOUND)
				.body(ApiErrorResponse.now(ex.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(error -> error.getDefaultMessage() == null ? "Invalid request" : error.getDefaultMessage())
				.orElse("Invalid request");
		return ResponseEntity
				.badRequest()
				.body(ApiErrorResponse.now(message));
	}
}
