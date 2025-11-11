package com.amalitech.task.service.controller;

import com.amalitech.common.security.dto.response.ApiResponse;
import com.amalitech.task.service.dto.TaskDTO;
import com.amalitech.task.service.exception.ResourceNotFoundException;
import com.amalitech.task.service.model.enums.TaskType;
import com.amalitech.task.service.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskControllerTest {

    @Mock
    private TaskService taskService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private TaskController taskController;

    private UUID taskId;
    private UUID userId;
    private TaskDTO testTaskDTO;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID();
        userId = UUID.randomUUID();

        testTaskDTO = new TaskDTO();
        testTaskDTO.setId(taskId);
        testTaskDTO.setTitle("Test Task");
        testTaskDTO.setDescription("Test Description");
    }

    @Test
    void testGetTaskById_Success() {
        when(taskService.getTaskById(taskId)).thenReturn(testTaskDTO);

        ResponseEntity<ApiResponse<TaskDTO>> response = taskController.getTaskById(taskId);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(taskId, response.getBody().getData().getId());
        verify(taskService, times(1)).getTaskById(taskId);
    }

    @Test
    void testGetTaskById_NotFound() {
        when(taskService.getTaskById(taskId)).thenThrow(new ResourceNotFoundException("Task not found"));

        assertThrows(ResourceNotFoundException.class, () -> {
            taskController.getTaskById(taskId);
        });

        verify(taskService, times(1)).getTaskById(taskId);
    }

    @Test
    void testGetPersonalizedTasks_Success() {
        List<TaskDTO> tasks = List.of(testTaskDTO);
        when(authentication.getName()).thenReturn(userId.toString());
        when(taskService.getPersonalizedTasks(userId, "PYTHON", TaskType.CODING, 5))
                .thenReturn(tasks);

        ResponseEntity<ApiResponse<List<TaskDTO>>> response = taskController.getPersonalizedTasks(
                "PYTHON", TaskType.CODING, 5, authentication
        );

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getData().size());
        verify(taskService, times(1)).getPersonalizedTasks(userId, "PYTHON", TaskType.CODING, 5);
    }

    @Test
    void testGetPersonalizedTasks_DefaultLimit() {
        List<TaskDTO> tasks = List.of(testTaskDTO);
        when(authentication.getName()).thenReturn(userId.toString());
        when(taskService.getPersonalizedTasks(userId, "JAVA", TaskType.ESSAY, 5))
                .thenReturn(tasks);

        ResponseEntity<ApiResponse<List<TaskDTO>>> response = taskController.getPersonalizedTasks(
                "JAVA", TaskType.ESSAY, 5, authentication
        );

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(taskService, times(1)).getPersonalizedTasks(userId, "JAVA", TaskType.ESSAY, 5);
    }

    @Test
    void testGetTaskById_ResponseStructure() {
        when(taskService.getTaskById(taskId)).thenReturn(testTaskDTO);

        ResponseEntity<ApiResponse<TaskDTO>> response = taskController.getTaskById(taskId);

        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getData());
        assertEquals("Task retrieved successfully.", response.getBody().getMessage());
    }

    @Test
    void testGetPersonalizedTasks_EmptyResult() {
        List<TaskDTO> emptyTasks = List.of();
        when(authentication.getName()).thenReturn(userId.toString());
        when(taskService.getPersonalizedTasks(userId, "PYTHON", TaskType.CODING, 5))
                .thenReturn(emptyTasks);

        ResponseEntity<ApiResponse<List<TaskDTO>>> response = taskController.getPersonalizedTasks(
                "PYTHON", TaskType.CODING, 5, authentication
        );

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody().getData().size());
    }

    @Test
    void testGetPersonalizedTasks_MultipleResults() {
        TaskDTO task1 = new TaskDTO();
        task1.setId(UUID.randomUUID());
        task1.setTitle("Task 1");

        TaskDTO task2 = new TaskDTO();
        task2.setId(UUID.randomUUID());
        task2.setTitle("Task 2");

        List<TaskDTO> tasks = List.of(task1, task2);
        when(authentication.getName()).thenReturn(userId.toString());
        when(taskService.getPersonalizedTasks(userId, "PYTHON", TaskType.CODING, 10))
                .thenReturn(tasks);

        ResponseEntity<ApiResponse<List<TaskDTO>>> response = taskController.getPersonalizedTasks(
                "PYTHON", TaskType.CODING, 10, authentication
        );

        assertNotNull(response);
        assertEquals(2, response.getBody().getData().size());
    }

    // ==================== GET USER TASKS (MY-TASKS) TESTS ====================

    @Test
    void testGetUserTasks_Success_DefaultPagination() {
        org.springframework.data.domain.Page<TaskDTO> pendingPage = 
            new org.springframework.data.domain.PageImpl<>(List.of(testTaskDTO), 
                org.springframework.data.domain.PageRequest.of(0, 10), 1);
        org.springframework.data.domain.Page<TaskDTO> completedPage = 
            new org.springframework.data.domain.PageImpl<>(List.of(), 
                org.springframework.data.domain.PageRequest.of(0, 10), 0);
        
        com.amalitech.task.service.dto.response.UserTasksResponse userTasksResponse = 
            new com.amalitech.task.service.dto.response.UserTasksResponse(pendingPage, completedPage);

        when(authentication.getName()).thenReturn(userId.toString());
        when(taskService.getUserTasksGroupedByStatus(userId, 0, 10, 0, 10))
                .thenReturn(userTasksResponse);

        ResponseEntity<ApiResponse<com.amalitech.task.service.dto.response.UserTasksResponse>> response = 
            taskController.getUserTasks(0, 10, 0, 10, authentication);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Tasks retrieved successfully", response.getBody().getMessage());
        assertEquals(1, response.getBody().getData().pending().getTotalElements());
        assertEquals(0, response.getBody().getData().completed().getTotalElements());
        verify(taskService, times(1)).getUserTasksGroupedByStatus(userId, 0, 10, 0, 10);
    }

    @Test
    void testGetUserTasks_Success_CustomPagination() {
        List<TaskDTO> pendingTasks = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            TaskDTO task = new TaskDTO();
            task.setId(UUID.randomUUID());
            task.setTitle("Pending Task " + i);
            pendingTasks.add(task);
        }

        List<TaskDTO> completedTasks = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            TaskDTO task = new TaskDTO();
            task.setId(UUID.randomUUID());
            task.setTitle("Completed Task " + i);
            completedTasks.add(task);
        }

        org.springframework.data.domain.Page<TaskDTO> pendingPage = 
            new org.springframework.data.domain.PageImpl<>(pendingTasks, 
                org.springframework.data.domain.PageRequest.of(1, 20), 40);
        org.springframework.data.domain.Page<TaskDTO> completedPage = 
            new org.springframework.data.domain.PageImpl<>(completedTasks, 
                org.springframework.data.domain.PageRequest.of(2, 15), 45);
        
        com.amalitech.task.service.dto.response.UserTasksResponse userTasksResponse = 
            new com.amalitech.task.service.dto.response.UserTasksResponse(pendingPage, completedPage);

        when(authentication.getName()).thenReturn(userId.toString());
        when(taskService.getUserTasksGroupedByStatus(userId, 1, 20, 2, 15))
                .thenReturn(userTasksResponse);

        ResponseEntity<ApiResponse<com.amalitech.task.service.dto.response.UserTasksResponse>> response = 
            taskController.getUserTasks(1, 20, 2, 15, authentication);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(40, response.getBody().getData().pending().getTotalElements());
        assertEquals(45, response.getBody().getData().completed().getTotalElements());
        verify(taskService, times(1)).getUserTasksGroupedByStatus(userId, 1, 20, 2, 15);
    }

    @Test
    void testGetUserTasks_ExtractsUserIdFromAuthentication() {
        org.springframework.data.domain.Page<TaskDTO> emptyPendingPage = 
            new org.springframework.data.domain.PageImpl<>(List.of(), 
                org.springframework.data.domain.PageRequest.of(0, 10), 0);
        org.springframework.data.domain.Page<TaskDTO> emptyCompletedPage = 
            new org.springframework.data.domain.PageImpl<>(List.of(), 
                org.springframework.data.domain.PageRequest.of(0, 10), 0);
        
        com.amalitech.task.service.dto.response.UserTasksResponse userTasksResponse = 
            new com.amalitech.task.service.dto.response.UserTasksResponse(emptyPendingPage, emptyCompletedPage);

        when(authentication.getName()).thenReturn(userId.toString());
        when(taskService.getUserTasksGroupedByStatus(userId, 0, 10, 0, 10))
                .thenReturn(userTasksResponse);

        taskController.getUserTasks(0, 10, 0, 10, authentication);

        verify(taskService, times(1)).getUserTasksGroupedByStatus(
                argThat(uuid -> uuid.equals(userId)), 
                eq(0), eq(10), eq(0), eq(10)
        );
    }

    @Test
    void testGetUserTasks_ResponseWrappedInApiResponse() {
        org.springframework.data.domain.Page<TaskDTO> pendingPage = 
            new org.springframework.data.domain.PageImpl<>(List.of(testTaskDTO), 
                org.springframework.data.domain.PageRequest.of(0, 10), 1);
        org.springframework.data.domain.Page<TaskDTO> completedPage = 
            new org.springframework.data.domain.PageImpl<>(List.of(), 
                org.springframework.data.domain.PageRequest.of(0, 10), 0);
        
        com.amalitech.task.service.dto.response.UserTasksResponse userTasksResponse = 
            new com.amalitech.task.service.dto.response.UserTasksResponse(pendingPage, completedPage);

        when(authentication.getName()).thenReturn(userId.toString());
        when(taskService.getUserTasksGroupedByStatus(userId, 0, 10, 0, 10))
                .thenReturn(userTasksResponse);

        ResponseEntity<ApiResponse<com.amalitech.task.service.dto.response.UserTasksResponse>> response = 
            taskController.getUserTasks(0, 10, 0, 10, authentication);

        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getData());
        assertTrue(response.getBody().isSuccess());
    }

    @Test
    void testGetUserTasks_PendingAndCompletedTasksPresent() {
        TaskDTO pendingTask = new TaskDTO();
        pendingTask.setId(UUID.randomUUID());
        pendingTask.setTitle("Pending Task");

        TaskDTO completedTask = new TaskDTO();
        completedTask.setId(UUID.randomUUID());
        completedTask.setTitle("Completed Task");

        org.springframework.data.domain.Page<TaskDTO> pendingPage = 
            new org.springframework.data.domain.PageImpl<>(List.of(pendingTask), 
                org.springframework.data.domain.PageRequest.of(0, 10), 1);
        org.springframework.data.domain.Page<TaskDTO> completedPage = 
            new org.springframework.data.domain.PageImpl<>(List.of(completedTask), 
                org.springframework.data.domain.PageRequest.of(0, 10), 1);
        
        com.amalitech.task.service.dto.response.UserTasksResponse userTasksResponse = 
            new com.amalitech.task.service.dto.response.UserTasksResponse(pendingPage, completedPage);

        when(authentication.getName()).thenReturn(userId.toString());
        when(taskService.getUserTasksGroupedByStatus(userId, 0, 10, 0, 10))
                .thenReturn(userTasksResponse);

        ResponseEntity<ApiResponse<com.amalitech.task.service.dto.response.UserTasksResponse>> response = 
            taskController.getUserTasks(0, 10, 0, 10, authentication);

        assertNotNull(response.getBody().getData().pending());
        assertNotNull(response.getBody().getData().completed());
        assertEquals(1, response.getBody().getData().pending().getTotalElements());
        assertEquals(1, response.getBody().getData().completed().getTotalElements());
    }

    @Test
    void testGetUserTasks_NoTasksFound_EmptyPages() {
        org.springframework.data.domain.Page<TaskDTO> emptyPendingPage = 
            new org.springframework.data.domain.PageImpl<>(List.of(), 
                org.springframework.data.domain.PageRequest.of(0, 10), 0);
        org.springframework.data.domain.Page<TaskDTO> emptyCompletedPage = 
            new org.springframework.data.domain.PageImpl<>(List.of(), 
                org.springframework.data.domain.PageRequest.of(0, 10), 0);
        
        com.amalitech.task.service.dto.response.UserTasksResponse userTasksResponse = 
            new com.amalitech.task.service.dto.response.UserTasksResponse(emptyPendingPage, emptyCompletedPage);

        when(authentication.getName()).thenReturn(userId.toString());
        when(taskService.getUserTasksGroupedByStatus(userId, 0, 10, 0, 10))
                .thenReturn(userTasksResponse);

        ResponseEntity<ApiResponse<com.amalitech.task.service.dto.response.UserTasksResponse>> response = 
            taskController.getUserTasks(0, 10, 0, 10, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody().getData().pending().getTotalElements());
        assertEquals(0, response.getBody().getData().completed().getTotalElements());
    }

    @Test
    void testGetUserTasks_OnlyPendingTasks_CompletedEmpty() {
        TaskDTO pendingTask = new TaskDTO();
        pendingTask.setId(UUID.randomUUID());
        pendingTask.setTitle("Pending Task");

        org.springframework.data.domain.Page<TaskDTO> pendingPage = 
            new org.springframework.data.domain.PageImpl<>(List.of(pendingTask), 
                org.springframework.data.domain.PageRequest.of(0, 10), 5);
        org.springframework.data.domain.Page<TaskDTO> emptyCompletedPage = 
            new org.springframework.data.domain.PageImpl<>(List.of(), 
                org.springframework.data.domain.PageRequest.of(0, 10), 0);
        
        com.amalitech.task.service.dto.response.UserTasksResponse userTasksResponse = 
            new com.amalitech.task.service.dto.response.UserTasksResponse(pendingPage, emptyCompletedPage);

        when(authentication.getName()).thenReturn(userId.toString());
        when(taskService.getUserTasksGroupedByStatus(userId, 0, 10, 0, 10))
                .thenReturn(userTasksResponse);

        ResponseEntity<ApiResponse<com.amalitech.task.service.dto.response.UserTasksResponse>> response = 
            taskController.getUserTasks(0, 10, 0, 10, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getData().pending().getTotalElements() > 0);
        assertEquals(0, response.getBody().getData().completed().getTotalElements());
    }

    @Test
    void testGetUserTasks_OnlyCompletedTasks_PendingEmpty() {
        TaskDTO completedTask = new TaskDTO();
        completedTask.setId(UUID.randomUUID());
        completedTask.setTitle("Completed Task");

        org.springframework.data.domain.Page<TaskDTO> emptyPendingPage = 
            new org.springframework.data.domain.PageImpl<>(List.of(), 
                org.springframework.data.domain.PageRequest.of(0, 10), 0);
        org.springframework.data.domain.Page<TaskDTO> completedPage = 
            new org.springframework.data.domain.PageImpl<>(List.of(completedTask), 
                org.springframework.data.domain.PageRequest.of(0, 10), 3);
        
        com.amalitech.task.service.dto.response.UserTasksResponse userTasksResponse = 
            new com.amalitech.task.service.dto.response.UserTasksResponse(emptyPendingPage, completedPage);

        when(authentication.getName()).thenReturn(userId.toString());
        when(taskService.getUserTasksGroupedByStatus(userId, 0, 10, 0, 10))
                .thenReturn(userTasksResponse);

        ResponseEntity<ApiResponse<com.amalitech.task.service.dto.response.UserTasksResponse>> response = 
            taskController.getUserTasks(0, 10, 0, 10, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody().getData().pending().getTotalElements());
        assertTrue(response.getBody().getData().completed().getTotalElements() > 0);
    }

    @Test
    void testGetUserTasks_MultipleTasksInBothPages() {
        List<TaskDTO> pendingTasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            TaskDTO task = new TaskDTO();
            task.setId(UUID.randomUUID());
            task.setTitle("Pending " + i);
            pendingTasks.add(task);
        }

        List<TaskDTO> completedTasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            TaskDTO task = new TaskDTO();
            task.setId(UUID.randomUUID());
            task.setTitle("Completed " + i);
            completedTasks.add(task);
        }

        org.springframework.data.domain.Page<TaskDTO> pendingPage = 
            new org.springframework.data.domain.PageImpl<>(pendingTasks, 
                org.springframework.data.domain.PageRequest.of(0, 10), 15);
        org.springframework.data.domain.Page<TaskDTO> completedPage = 
            new org.springframework.data.domain.PageImpl<>(completedTasks, 
                org.springframework.data.domain.PageRequest.of(0, 10), 10);
        
        com.amalitech.task.service.dto.response.UserTasksResponse userTasksResponse = 
            new com.amalitech.task.service.dto.response.UserTasksResponse(pendingPage, completedPage);

        when(authentication.getName()).thenReturn(userId.toString());
        when(taskService.getUserTasksGroupedByStatus(userId, 0, 10, 0, 10))
                .thenReturn(userTasksResponse);

        ResponseEntity<ApiResponse<com.amalitech.task.service.dto.response.UserTasksResponse>> response = 
            taskController.getUserTasks(0, 10, 0, 10, authentication);

        assertEquals(10, response.getBody().getData().pending().getContent().size());
        assertEquals(10, response.getBody().getData().completed().getContent().size());
        assertEquals(15, response.getBody().getData().pending().getTotalElements());
        assertEquals(10, response.getBody().getData().completed().getTotalElements());
    }

    @Test
    void testGetUserTasks_ResponseMessage() {
        org.springframework.data.domain.Page<TaskDTO> pendingPage = 
            new org.springframework.data.domain.PageImpl<>(List.of(testTaskDTO), 
                org.springframework.data.domain.PageRequest.of(0, 10), 1);
        org.springframework.data.domain.Page<TaskDTO> completedPage = 
            new org.springframework.data.domain.PageImpl<>(List.of(), 
                org.springframework.data.domain.PageRequest.of(0, 10), 0);
        
        com.amalitech.task.service.dto.response.UserTasksResponse userTasksResponse = 
            new com.amalitech.task.service.dto.response.UserTasksResponse(pendingPage, completedPage);

        when(authentication.getName()).thenReturn(userId.toString());
        when(taskService.getUserTasksGroupedByStatus(userId, 0, 10, 0, 10))
                .thenReturn(userTasksResponse);

        ResponseEntity<ApiResponse<com.amalitech.task.service.dto.response.UserTasksResponse>> response = 
            taskController.getUserTasks(0, 10, 0, 10, authentication);

        assertEquals("Tasks retrieved successfully", response.getBody().getMessage());
    }

    @Test
    void testGetUserTasks_LargePageSize() {
        List<TaskDTO> pendingTasks = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            TaskDTO task = new TaskDTO();
            task.setId(UUID.randomUUID());
            task.setTitle("Pending " + i);
            pendingTasks.add(task);
        }

        org.springframework.data.domain.Page<TaskDTO> pendingPage = 
            new org.springframework.data.domain.PageImpl<>(pendingTasks, 
                org.springframework.data.domain.PageRequest.of(0, 50), 100);
        org.springframework.data.domain.Page<TaskDTO> completedPage = 
            new org.springframework.data.domain.PageImpl<>(List.of(), 
                org.springframework.data.domain.PageRequest.of(0, 10), 0);
        
        com.amalitech.task.service.dto.response.UserTasksResponse userTasksResponse = 
            new com.amalitech.task.service.dto.response.UserTasksResponse(pendingPage, completedPage);

        when(authentication.getName()).thenReturn(userId.toString());
        when(taskService.getUserTasksGroupedByStatus(userId, 0, 50, 0, 10))
                .thenReturn(userTasksResponse);

        ResponseEntity<ApiResponse<com.amalitech.task.service.dto.response.UserTasksResponse>> response = 
            taskController.getUserTasks(0, 50, 0, 10, authentication);

        assertEquals(50, response.getBody().getData().pending().getContent().size());
        assertEquals(100, response.getBody().getData().pending().getTotalElements());
    }
}
