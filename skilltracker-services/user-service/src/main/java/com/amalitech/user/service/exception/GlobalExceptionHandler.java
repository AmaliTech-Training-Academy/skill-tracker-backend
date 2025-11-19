package com.amalitech.user.service.exception;

import com.amalitech.common.security.dto.response.ApiError;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;

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
                null,
                request.getRequestURI(),
                null,
                getTraceId()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(UserSuspendedException.class)
    public ResponseEntity<ApiError> handleUserSuspendedException(RefreshTokenException ex, HttpServletRequest request) {
        ApiError error = ApiError.of(
                HttpStatus.UNAUTHORIZED.value(),
                "User Suspended",
                null,
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
                "Email already exists",
                null,
                request.getRequestURI(),
                null,
                getTraceId()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(UnverifiedUserException.class)
    public ResponseEntity<ApiError> UnverifiedUserException(UnverifiedUserException ex, HttpServletRequest request) {
        ApiError error = ApiError.of(
                HttpStatus.CONFLICT.value(),
                ex.getMessage(),
                null,
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
                "user not found",
                null,
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
                "invalid credentials",
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
                HttpStatus.UNAUTHORIZED.value(),
                "Invalid token.",
                null,
                request.getRequestURI(),
                null,
                getTraceId()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
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

    @ExceptionHandler(NoSkillsFoundException.class)
    public ResponseEntity<ApiError> handleNoSkillsFound(NoSkillsFoundException ex, HttpServletRequest request) {
        ApiError error = ApiError.of(
                HttpStatus.NOT_FOUND.value(),
                "No skills found",
                ex.getMessage(),
                request.getRequestURI(),
                null,
                getTraceId()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(OnboardingAlreadyCompletedException.class)
    public ResponseEntity<ApiError> handleOnboardingAlreadyCompleted(
            OnboardingAlreadyCompletedException ex, HttpServletRequest request
    ) {
        ApiError error = ApiError.of(
                HttpStatus.CONFLICT.value(),
                "Onboarding already completed",
                ex.getMessage(),
                request.getRequestURI(),
                null,
                getTraceId()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handles exceptions related to duplicate resource creation.
     * Returns a 409 CONFLICT.
     */
    @ExceptionHandler(DuplicateSkillException.class)
    public ResponseEntity<ApiError> handleDuplicateSkill(
            DuplicateSkillException ex, HttpServletRequest request) {

        ApiError apiError = ApiError.of(
                HttpStatus.CONFLICT.value(),
                "Skill already exists",
                ex.getMessage(),
                request.getRequestURI(),
                null,
                getTraceId()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(apiError);
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

    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ResponseEntity<ApiError> handleDatabaseException(DataAccessException ex, HttpServletRequest request) {

        ApiError error = ApiError.of(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "Database unavailable. Please try again later.",
                "A problem occurred while communicating with the database.",
                request.getRequestURI(),
                null,
                getTraceId()
        );

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiError.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiError.FieldError(error.getField(), error.getDefaultMessage()))
                .toList();

        String message = fieldErrors.isEmpty() ? "Validation failed" : fieldErrors.get(0).message();

        ApiError error = ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                message,
                "Request validation failed",
                request.getRequestURI(),
                fieldErrors,
                getTraceId()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String message = "Invalid request format";
        List<ApiError.FieldError> fieldErrors = null;

        if (ex.getCause() instanceof InvalidFormatException invalidFormatEx) {
            if (invalidFormatEx.getTargetType().isEnum()) {
                String fieldName = invalidFormatEx.getPath().get(0).getFieldName();
                String enumValues = String.join(", ",
                        Arrays.stream(invalidFormatEx.getTargetType().getEnumConstants())
                                .map(Object::toString)
                                .toArray(String[]::new));

                message = String.format("Invalid value for field '%s'", fieldName);
                fieldErrors = List.of(
                        new ApiError.FieldError(fieldName, "Allowed values: " + enumValues)
                );
            }
        }

        ApiError error = ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                message,
                "Failed to parse request body",
                request.getRequestURI(),
                fieldErrors,
                getTraceId()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}