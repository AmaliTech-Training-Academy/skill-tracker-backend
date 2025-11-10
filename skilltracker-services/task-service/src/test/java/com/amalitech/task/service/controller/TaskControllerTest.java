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
}
