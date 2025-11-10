package com.amalitech.task.service.exception.handler;

import com.amalitech.common.security.dto.response.ApiError;
import com.amalitech.task.service.exception.ResourceNotFoundException;
import com.amalitech.task.service.exception.SkillsNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles 404 Not Found errors.
     * This is triggered by your custom ResourceNotFoundException.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {

        log.warn("Resource not found: {}", ex.getMessage());

        ApiError error = ApiError.of(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                null,
                request.getRequestURI(),
                null,
                getTraceId()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handles 400 Bad Request errors.
     * This is triggered when @Valid on a DTO fails.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<ApiError.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ApiError.FieldError(
                        error.getField(),
                        error.getDefaultMessage()
                ))
                .collect(Collectors.toList());

        log.warn("Validation failed with {} error(s)", fieldErrors.size());

        ApiError error = ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                "One or more fields have validation errors",
                request.getRequestURI(),
                fieldErrors,
                getTraceId()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(SkillsNotFoundException.class)
    public ResponseEntity<ApiError> handleSkillsNotFoundException(
            SkillsNotFoundException ex, HttpServletRequest request) {

        ApiError error = ApiError.of(
                HttpStatus.NOT_FOUND.value(),
                "Skills not found",
                ex.getMessage(),
                request.getRequestURI(),
                null,
                getTraceId()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiError> handleEntityNotFoundException(
            EntityNotFoundException ex, HttpServletRequest request) {

        ApiError error = ApiError.of(
                HttpStatus.NOT_FOUND.value(),
                "Entity not found",
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
     * Falls back to "N/A" if no trace ID is available.
     */
    private String getTraceId() {
        String traceId = MDC.get("traceId");
        return (traceId != null && !traceId.isBlank()) ? traceId : null;
    }
}