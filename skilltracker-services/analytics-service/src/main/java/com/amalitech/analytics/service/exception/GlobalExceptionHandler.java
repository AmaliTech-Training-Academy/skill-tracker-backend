package com.amalitech.analytics.service.exception;

import com.amalitech.common.security.dto.response.ApiError;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles EntityNotFoundException and maps to HTTP 404 NOT_FOUND.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiError> handleEntityNotFoundException(EntityNotFoundException ex, WebRequest request) {
        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "Resource not found.",
                ex.getMessage(),
                getServletPath(request)
        );
    }

    /**
     * Handles InvalidGoalArgumentException and maps to HTTP 400 BAD_REQUEST.
     */
    @ExceptionHandler(InvalidGoalArgumentException.class)
    public ResponseEntity<ApiError> handleInvalidGoalArgumentException(InvalidGoalArgumentException ex, WebRequest request) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid input for business logic.",
                ex.getMessage(),
                getServletPath(request)
        );
    }


    /**
     * Handles @Valid violations (e.g., @NotNull, @Min) and maps to HTTP 400.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        List<ApiError.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiError.FieldError(error.getField(), error.getDefaultMessage()))
                .collect(Collectors.toList());

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Validation Failed",
                "One or more request parameters failed validation.",
                getServletPath(request),
                fieldErrors
        );
    }

    /**
     * Handles UUID parsing errors and maps to HTTP 400.
     * Catches the case where 'UUID.fromString()' fails.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid Argument",
                ex.getMessage(),
                getServletPath(request)
        );
    }

    /**
     * Handles database integrity violations (NOT NULL, unique constraints) and maps to HTTP 400.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(DataIntegrityViolationException ex, WebRequest request) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Data Integrity Violation",
                ex.getMostSpecificCause().getMessage(),
                getServletPath(request)
        );
    }

    /**
     * Handles all unexpected runtime exceptions. Maps to HTTP 500 INTERNAL_SERVER_ERROR.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAllExceptions(Exception ex, WebRequest request) {
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                getServletPath(request)
        );
    }

    // --- Helper Methods ---

    private ResponseEntity<ApiError> buildErrorResponse(
            HttpStatus status,
            String message,
            String detail,
            String instance) {
        return buildErrorResponse(status, message, detail, instance, List.of());
    }

    private ResponseEntity<ApiError> buildErrorResponse(
            HttpStatus status,
            String message,
            String detail,
            String instance,
            List<ApiError.FieldError> errors) {

        String traceId = UUID.randomUUID().toString();

        ApiError error = ApiError.of(
                status.value(),
                message,
                detail,
                instance,
                errors,
                traceId
        );
        return new ResponseEntity<>(error, status);
    }

    private String getServletPath(WebRequest request) {
        if (request instanceof ServletWebRequest swr) {
            return swr.getRequest().getRequestURI();
        }
        return "N/A";
    }
}