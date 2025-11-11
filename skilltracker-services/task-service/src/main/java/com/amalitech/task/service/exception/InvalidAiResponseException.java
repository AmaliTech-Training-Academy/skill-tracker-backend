package com.amalitech.task.service.exception;

/**
 * Exception thrown when the AI service response is malformed, incomplete,
 * or missing required fields.
 *
 * This indicates that while the AI service responded, the response structure
 * was not as expected and cannot be processed. Results in a 502 Bad Gateway
 * HTTP response.
 */
public class InvalidAiResponseException extends RuntimeException {
    public InvalidAiResponseException(String message) {
        super(message);
    }

    public InvalidAiResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
