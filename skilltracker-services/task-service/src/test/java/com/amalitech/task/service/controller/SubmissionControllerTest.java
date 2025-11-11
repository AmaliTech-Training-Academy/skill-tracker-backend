package com.amalitech.task.service.controller;

import com.amalitech.common.security.dto.response.ApiResponse;
import com.amalitech.task.service.dto.TaskSubmissionDTO;
import com.amalitech.task.service.dto.request.SubmitAnswerRequest;
import com.amalitech.task.service.dto.response.SubmissionResponse;
import com.amalitech.task.service.exception.ResourceNotFoundException;
import com.amalitech.task.service.exception.InvalidUserIdException;
import com.amalitech.task.service.model.enums.SubmissionStatus;
import com.amalitech.task.service.model.submission.impl.CodingSubmissionAnswer;
import com.amalitech.task.service.service.SubmissionService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionControllerTest {

    @Mock
    private SubmissionService submissionService;

    @InjectMocks
    private SubmissionController submissionController;

    private UUID submissionId;
    private UUID taskId;
    private UUID userId;
    private TaskSubmissionDTO testSubmissionDTO;
    private SubmitAnswerRequest submitRequest;

    @BeforeEach
    void setUp() {
        submissionId = UUID.randomUUID();
        taskId = UUID.randomUUID();
        userId = UUID.randomUUID();

        testSubmissionDTO = new TaskSubmissionDTO();
        testSubmissionDTO.setId(submissionId);
        testSubmissionDTO.setStatus(SubmissionStatus.PENDING);

        CodingSubmissionAnswer answer = new CodingSubmissionAnswer("test answer code", 71);
        submitRequest = new SubmitAnswerRequest(taskId, answer);
    }

    @Test
    void testSubmitTask_Success() {
        when(submissionService.createSubmission(submitRequest, userId))
                .thenReturn(testSubmissionDTO);

        ResponseEntity<ApiResponse<SubmissionResponse>> response = submissionController.submitTask(
                userId.toString(), submitRequest
        );

        assertNotNull(response);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(submissionId, response.getBody().getData().submissionId());
        assertEquals(SubmissionStatus.PENDING, response.getBody().getData().status());
        verify(submissionService, times(1)).createSubmission(submitRequest, userId);
    }

    @Test
    void testSubmitTask_InvalidUserIdFormat() {
        assertThrows(InvalidUserIdException.class, () -> {
            submissionController.submitTask("invalid-uuid", submitRequest);
        });

        verify(submissionService, never()).createSubmission(any(), any());
    }

    @Test
    void testGetSubmission_Success() {
        when(submissionService.getSubmissionById(submissionId))
                .thenReturn(testSubmissionDTO);

        ResponseEntity<ApiResponse<TaskSubmissionDTO>> response = submissionController.getSubmission(submissionId);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(submissionId, response.getBody().getData().getId());
        verify(submissionService, times(1)).getSubmissionById(submissionId);
    }

    @Test
    void testGetSubmission_NotFound() {
        when(submissionService.getSubmissionById(submissionId))
                .thenThrow(new ResourceNotFoundException("Submission not found"));

        assertThrows(ResourceNotFoundException.class, () -> {
            submissionController.getSubmission(submissionId);
        });

        verify(submissionService, times(1)).getSubmissionById(submissionId);
    }

    @Test
    void testSubmitTask_ResponseHttp202Status() {
        when(submissionService.createSubmission(submitRequest, userId))
                .thenReturn(testSubmissionDTO);

        ResponseEntity<ApiResponse<SubmissionResponse>> response = submissionController.submitTask(
                userId.toString(), submitRequest
        );

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertEquals(202, response.getStatusCodeValue());
    }

    @Test
    void testSubmitTask_ResponseMessage() {
        when(submissionService.createSubmission(submitRequest, userId))
                .thenReturn(testSubmissionDTO);

        ResponseEntity<ApiResponse<SubmissionResponse>> response = submissionController.submitTask(
                userId.toString(), submitRequest
        );

        assertEquals("Submission accepted for evaluation.", response.getBody().getMessage());
    }

    @Test
    void testGetSubmission_ResponseMessage() {
        when(submissionService.getSubmissionById(submissionId))
                .thenReturn(testSubmissionDTO);

        ResponseEntity<ApiResponse<TaskSubmissionDTO>> response = submissionController.getSubmission(submissionId);

        assertEquals("Submission retrieved successfully.", response.getBody().getMessage());
    }

    @Test
    void testSubmitTask_EventProducerCalled() {
        when(submissionService.createSubmission(submitRequest, userId))
                .thenReturn(testSubmissionDTO);

        submissionController.submitTask(userId.toString(), submitRequest);

        verify(submissionService, times(1)).createSubmission(submitRequest, userId);
    }

    @Test
    void testSubmitTask_CorrectUserIdParsed() {
        UUID testUserId = UUID.randomUUID();
        when(submissionService.createSubmission(submitRequest, testUserId))
                .thenReturn(testSubmissionDTO);

        submissionController.submitTask(testUserId.toString(), submitRequest);

        verify(submissionService, times(1)).createSubmission(submitRequest, testUserId);
    }

    @Test
    void testGetSubmission_ReturnsCorrectSubmissionId() {
        UUID testSubmissionId = UUID.randomUUID();
        TaskSubmissionDTO expectedDTO = new TaskSubmissionDTO();
        expectedDTO.setId(testSubmissionId);
        expectedDTO.setStatus(SubmissionStatus.COMPLETED);

        when(submissionService.getSubmissionById(testSubmissionId))
                .thenReturn(expectedDTO);

        ResponseEntity<ApiResponse<TaskSubmissionDTO>> response = submissionController.getSubmission(testSubmissionId);

        assertEquals(testSubmissionId, response.getBody().getData().getId());
    }
}
