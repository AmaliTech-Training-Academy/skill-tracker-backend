package com.amalitech.task.service.exception.handler;

import com.amalitech.common.security.dto.response.ApiError;
import com.amalitech.task.service.exception.ResourceNotFoundException;
import com.amalitech.task.service.exception.SkillsNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        when(request.getRequestURI()).thenReturn("/api/v1/tasks");
    }

    @Test
    void testHandleResourceNotFoundException() {
        ResourceNotFoundException exception = new ResourceNotFoundException("Task not found");

        ResponseEntity<ApiError> response = globalExceptionHandler.handleResourceNotFoundException(exception, request);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().status());
        assertTrue(response.getBody().message().contains("Task not found"));
    }

    @Test
    void testHandleSkillsNotFoundException() {
        SkillsNotFoundException exception = new SkillsNotFoundException(UUID.randomUUID());

        ResponseEntity<ApiError> response = globalExceptionHandler.handleSkillsNotFoundException(exception, request);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().status());
    }

    @Test
    void testHandleEntityNotFoundException() {
        EntityNotFoundException exception = new EntityNotFoundException("Entity not found");

        ResponseEntity<ApiError> response = globalExceptionHandler.handleEntityNotFoundException(exception, request);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().status());
        assertEquals("Entity not found", response.getBody().message());
    }

    @Test
    void testHandleGlobalException() {
        Exception exception = new RuntimeException("Unexpected error");

        ResponseEntity<ApiError> response = globalExceptionHandler.handleGlobalException(exception, request);

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().status());
        assertTrue(response.getBody().message().contains("unexpected error"));
    }

    @Test
    void testHandleResourceNotFoundException_IncludesUri() {
        ResourceNotFoundException exception = new ResourceNotFoundException("Task not found: 550e8400-e29b-41d4-a716-446655440000");

        ResponseEntity<ApiError> response = globalExceptionHandler.handleResourceNotFoundException(exception, request);

        assertNotNull(response.getBody());
        assertNotNull(response.getBody().instance());
    }

    @Test
    void testHandleGlobalException_IncludesUri() {
        Exception exception = new RuntimeException("Internal error");

        ResponseEntity<ApiError> response = globalExceptionHandler.handleGlobalException(exception, request);

        assertNotNull(response.getBody());
        assertNotNull(response.getBody().instance());
    }

    @Test
    void testHandleResourceNotFoundException_ExceptionMessagePreserved() {
        String errorMessage = "Submission with ID 550e8400-e29b-41d4-a716-446655440001 not found";
        ResourceNotFoundException exception = new ResourceNotFoundException(errorMessage);

        ResponseEntity<ApiError> response = globalExceptionHandler.handleResourceNotFoundException(exception, request);

        assertNotNull(response.getBody());
        assertTrue(response.getBody().toString().contains(errorMessage));
    }

    @Test
    void testHandleSkillsNotFoundException_ResponseStructure() {
        SkillsNotFoundException exception = new SkillsNotFoundException(UUID.randomUUID());

        ResponseEntity<ApiError> response = globalExceptionHandler.handleSkillsNotFoundException(exception, request);

        assertNotNull(response.getBody());
        assertEquals("Skills not found", response.getBody().message());
        assertNotNull(response.getBody().instance());
    }

    @Test
    void testHandleEntityNotFoundException_ResponseStructure() {
        EntityNotFoundException exception = new EntityNotFoundException("User entity not found");

        ResponseEntity<ApiError> response = globalExceptionHandler.handleEntityNotFoundException(exception, request);

        assertNotNull(response.getBody());
        assertEquals("Entity not found", response.getBody().message());
        assertEquals(404, response.getBody().status());
    }

    @Test
    void testHandleGlobalException_CatchAllFunctionality() {
        IllegalStateException exception = new IllegalStateException("Invalid state");

        ResponseEntity<ApiError> response = globalExceptionHandler.handleGlobalException(exception, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().status());
        assertEquals("An unexpected error occurred", response.getBody().message());
    }

    @Test
    void testHandleResourceNotFoundException_CorrectStatus() {
        ResourceNotFoundException exception = new ResourceNotFoundException("Not found");

        ResponseEntity<ApiError> response = globalExceptionHandler.handleResourceNotFoundException(exception, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().status());
    }
}
