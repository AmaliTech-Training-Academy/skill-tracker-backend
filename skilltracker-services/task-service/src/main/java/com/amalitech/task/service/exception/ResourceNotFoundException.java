package com.amalitech.task.service.exception;

/**
 * A custom runtime exception used throughout the Task Service to indicate that a requested
 * resource (e.g., a Task, a Submission, a User View) could not be located in the persistence layer.
 * <p>
 * This exception is typically mapped by a global exception handler (e.g., using {@code @ControllerAdvice})
 * to an appropriate HTTP status code, such as 404 Not Found, providing a clean API contract for clients.
 */
public class ResourceNotFoundException extends RuntimeException {
    /**
     * Constructs a new ResourceNotFoundException with the specified detail message.
     * <p>
     * The detail message provides context about the resource that was not found.
     *
     * @param message The detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}