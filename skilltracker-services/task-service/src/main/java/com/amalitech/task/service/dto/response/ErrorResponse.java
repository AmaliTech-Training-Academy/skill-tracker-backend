package com.amalitech.task.service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * A standardized DTO for returning API errors.
 *
 * @param statusCode The HTTP status code (e.g., 404, 400).
 * @param message A user-friendly error message.
 * @param details Optional map of more specific errors (e.g., validation fields).
 * @param timestamp The time the error occurred.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        int statusCode,
        String message,
        Map<String, String> details,
        LocalDateTime timestamp
) {
    public ErrorResponse(HttpStatus status, String message) {
        this(status.value(), message, null, LocalDateTime.now());
    }

    public ErrorResponse(HttpStatus status, String message, Map<String, String> details) {
        this(status.value(), message, details, LocalDateTime.now());
    }
}
