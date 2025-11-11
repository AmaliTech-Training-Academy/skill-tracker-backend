package com.amalitech.task.service.exception;

/**
 * Exception thrown when parsing of the AI service response JSON fails.
 *
 * This occurs when the response structure exists but contains invalid JSON
 * or cannot be deserialized into expected objects. Results in a 502 Bad Gateway
 * HTTP response.
 */
public class AiResponseParsingException extends RuntimeException {
    public AiResponseParsingException(String message) {
        super(message);
    }

    public AiResponseParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
