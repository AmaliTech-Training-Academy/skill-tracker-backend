package com.amalitech.notification.service.exception.handler;

import com.amalitech.common.security.dto.response.ApiError;
import com.amalitech.notification.service.exception.NotificationNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for the notification-service.
 * 
 * Centralizes exception handling across all controllers and provides
 * consistent error responses using the standardized ApiError format.
 * 
 * All responses include request tracing via MDC trace ID for debugging
 * and monitoring purposes.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles 404 Not Found when notification is not found or doesn't belong to user.
     */
    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<ApiError> handleNotificationNotFoundException(
            NotificationNotFoundException ex, HttpServletRequest request) {

        log.warn("Notification not found or access denied: {}", ex.getMessage());

        ApiError error = ApiError.of(
                HttpStatus.NOT_FOUND.value(),
                "Notification not found",
                ex.getMessage(),
                request.getRequestURI(),
                null,
                getTraceId()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
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
     * Falls back to null if no trace ID is available.
     */
    private String getTraceId() {
        String traceId = MDC.get("traceId");
        return (traceId != null && !traceId.isBlank()) ? traceId : null;
    }
}
