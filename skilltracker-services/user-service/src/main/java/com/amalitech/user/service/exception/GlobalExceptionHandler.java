package com.amalitech.user.service.exception;

import com.amalitech.common.security.dto.response.ApiError;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler for the application. Catches all exceptions and returns consistent
 * ApiResponse.error responses with appropriate HTTP status codes.
 * Extends ResponseEntityExceptionHandler to handle Spring-specific exceptions.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private String getTraceId() {
        String traceId = MDC.get("traceId");
        return (traceId != null && !traceId.isBlank()) ? traceId : "...";
    }

    @ExceptionHandler(RefreshTokenException.class)
    public ResponseEntity<ApiError> handleRefreshTokenException(RefreshTokenException ex, HttpServletRequest request) {
        ApiError error = ApiError.of(
                HttpStatus.UNAUTHORIZED.value(),
                "Invalid refresh token",
                ex.getMessage(),
                request.getRequestURI(),
                null,
                getTraceId()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleEmailAlreadyExistsException(EmailAlreadyExistsException ex, HttpServletRequest request) {
        ApiError error = ApiError.of(
                HttpStatus.CONFLICT.value(),
                ex.getMessage(),
                ex.getMessage(),
                request.getRequestURI(),
                null,
                getTraceId()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiError> handleUserNotFound(UserNotFoundException ex, HttpServletRequest request) {
        ApiError error = ApiError.of(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                ex.getMessage(),
                request.getRequestURI(),
                null,
                getTraceId()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        ApiError error = ApiError.of(
                HttpStatus.UNAUTHORIZED.value(),
                ex.getMessage(),
                "The email or password provided is incorrect.",
                request.getRequestURI(),
                null,
                getTraceId()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ApiError> handleInvalidPassword(InvalidPasswordException ex, HttpServletRequest request) {
        ApiError error = ApiError.of(
                HttpStatus.UNAUTHORIZED.value(),
                "Invalid credentials",
                ex.getMessage(),
                request.getRequestURI(),
                null,
                getTraceId()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiError> handleInvalidToken(InvalidTokenException ex, HttpServletRequest request) {
        ApiError error = ApiError.of(
                HttpStatus.NOT_FOUND.value(),
                "Invalid token.",
                ex.getMessage(),
                request.getRequestURI(),
                null,
                getTraceId()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiError.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ApiError.FieldError(error.getField(), error.getDefaultMessage()))
                .collect(Collectors.toList());

        ApiError error = ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                "One or more fields are invalid",
                request.getRequestURI(),
                fieldErrors,
                getTraceId()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatusException(ResponseStatusException ex, HttpServletRequest request) {
        ApiError error = ApiError.of(
                ex.getStatusCode().value(),
                ex.getReason(),
                ex.getMessage(),
                request.getRequestURI(),
                null,
                getTraceId()
        );
        return ResponseEntity.status(ex.getStatusCode()).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneralException(Exception ex, HttpServletRequest request) {
        ex.printStackTrace();
        ApiError error = ApiError.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred",
                ex.getMessage(),
                request.getRequestURI(),
                null,
                getTraceId()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}