package com.amalitech.task.service.exception;

/**
 * Exception thrown when the user ID from the security principal is invalid
 * or cannot be parsed as a UUID.
 *
 * This indicates a client-side issue with the authentication token and
 * results in a 400 Bad Request HTTP response.
 */
public class InvalidUserIdException extends RuntimeException {
    public InvalidUserIdException(String message) {
        super(message);
    }

    public InvalidUserIdException(String message, Throwable cause) {
        super(message, cause);
    }
}
