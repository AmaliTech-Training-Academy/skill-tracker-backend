package com.amalitech.feedback.service.exception.handler;

import com.amalitech.common.security.dto.response.ApiError;
import com.amalitech.feedback.service.exception.AiResponseParsingException;
import com.amalitech.feedback.service.exception.InvalidAiResponseException;
import com.amalitech.feedback.service.exception.InvalidTaskException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles 400 Bad Request when task is missing required data.
     */
    @ExceptionHandler(InvalidTaskException.class)
    public ResponseEntity<ApiError> handleInvalidTaskException(
            InvalidTaskException ex, HttpServletRequest request) {

        log.warn("Invalid task configuration: {}", ex.getMessage());

        ApiError error = ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid task",
                ex.getMessage(),
                request.getRequestURI(),
                null,
                getTraceId()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles 502 Bad Gateway errors when AI response is malformed.
     */
    @ExceptionHandler(InvalidAiResponseException.class)
    public ResponseEntity<ApiError> handleInvalidAiResponseException(
            InvalidAiResponseException ex, HttpServletRequest request) {

        log.error("Invalid AI response: {}", ex.getMessage(), ex);

        ApiError error = ApiError.of(
                HttpStatus.BAD_GATEWAY.value(),
                "Invalid AI response",
                "Received unexpected response format from AI service.",
                request.getRequestURI(),
                null,
                getTraceId()
        );

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error);
    }

    /**
     * Handles 502 Bad Gateway errors when AI response parsing fails.
     */
    @ExceptionHandler(AiResponseParsingException.class)
    public ResponseEntity<ApiError> handleAiResponseParsingException(
            AiResponseParsingException ex, HttpServletRequest request) {

        log.error("Failed to parse AI response: {}", ex.getMessage(), ex);

        ApiError error = ApiError.of(
                HttpStatus.BAD_GATEWAY.value(),
                "AI response parsing failed",
                "Failed to parse response from AI service.",
                request.getRequestURI(),
                null,
                getTraceId()
        );

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error);
    }

    /**
     * Handles 500 Internal Server Errors.
     * This is a "catch-all" for any other unexpected exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGlobalException(
            Exception ex, HttpServletRequest request) {

        log.error("An unexpected error occurred: {}", ex.getMessage(), ex);

        ApiError error = ApiError.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred",
                "Please try again later. If the problem persists, contact support.",
                request.getRequestURI(),
                null,
                getTraceId()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Retrieves the trace ID from MDC for request tracing.
     * Falls back to "N/A" if no trace ID is available.
     */
    private String getTraceId() {
        String traceId = MDC.get("traceId");
        return (traceId != null && !traceId.isBlank()) ? traceId : null;
    }
}
