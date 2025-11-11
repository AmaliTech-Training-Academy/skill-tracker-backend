package com.amalitech.task.service.exception;

/**
 * Exception thrown when the AI service (OpenAI) API call fails, times out,
 * or returns an error response.
 *
 * This typically indicates a problem with external service communication
 * and should result in a 502 Bad Gateway HTTP response.
 */
public class AiServiceException extends RuntimeException {
    public AiServiceException(String message) {
        super(message);
    }

    public AiServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
