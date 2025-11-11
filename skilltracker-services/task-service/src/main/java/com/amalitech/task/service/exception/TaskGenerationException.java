package com.amalitech.task.service.exception;

/**
 * Exception thrown when the task generation process fails.
 *
 * This can occur during various stages of task generation, including
 * AI content generation, data validation, or persistence. Results in a
 * 500 Internal Server Error HTTP response.
 */
public class TaskGenerationException extends RuntimeException {
    public TaskGenerationException(String message) {
        super(message);
    }

    public TaskGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
