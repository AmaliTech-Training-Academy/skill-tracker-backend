package com.amalitech.feedback.service.exception;

/**
 * Exception thrown when a task has missing or invalid required data.
 *
 * This typically indicates a problem with the task configuration or data integrity
 * and should result in a 400 Bad Request HTTP response.
 */
public class InvalidTaskException extends RuntimeException {
    public InvalidTaskException(String message) {
        super(message);
    }

    public InvalidTaskException(String message, Throwable cause) {
        super(message, cause);
    }
}
