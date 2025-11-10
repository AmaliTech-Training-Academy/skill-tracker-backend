package com.amalitech.task.service.controller;

import com.amalitech.common.security.dto.response.ApiResponse;
import com.amalitech.task.service.dto.request.GenerateTaskRequest;
import com.amalitech.task.service.dto.response.AdminTaskDetailResponse;
import com.amalitech.task.service.dto.response.AdminTaskSummaryResponse;
import com.amalitech.task.service.exception.ResourceNotFoundException;
import com.amalitech.task.service.model.enums.TaskDifficulty;
import com.amalitech.task.service.model.enums.TaskType;
import com.amalitech.task.service.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskAdminControllerTest {

    @Mock
    private TaskService taskService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private TaskAdminController taskAdminController;

    private UUID taskId;
    private UUID adminUserId;
    private AdminTaskSummaryResponse testSummaryResponse;
    private AdminTaskDetailResponse testDetailResponse;
    private GenerateTaskRequest generateRequest;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID();
        adminUserId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        testSummaryResponse = new AdminTaskSummaryResponse(
                taskId,
                "Test Task",
                TaskType.CODING.name(),
                TaskDifficulty.INTERMEDIATE.name(),
                1,
                true,
                now
        );

        testDetailResponse = new AdminTaskDetailResponse(
                taskId,
                UUID.randomUUID(),
                "Test Task",
                "Test Description",
                "Test Skill",
                TaskType.CODING.name(),
                TaskDifficulty.INTERMEDIATE.name(),
                null,
                1,
                true,
                30,
                100,
                now,
                now
        );

        generateRequest = new GenerateTaskRequest(
                adminUserId,
                TaskType.CODING,
                "PYTHON",
                TaskDifficulty.INTERMEDIATE,
                "String Manipulation",
                "Python"
        );

        lenient().when(authentication.getName()).thenReturn(adminUserId.toString());
    }

    // ==================== GET ALL TASKS TESTS ====================

    @Test
    void testGetAllTasks_Success() {
        Pageable pageable = PageRequest.of(0, 20);
        List<AdminTaskSummaryResponse> tasks = List.of(testSummaryResponse);
        Page<AdminTaskSummaryResponse> taskPage = new PageImpl<>(tasks, pageable, 1);

        when(taskService.getAllTasksForAdmin(pageable)).thenReturn(taskPage);

        ResponseEntity<ApiResponse<Page<AdminTaskSummaryResponse>>> response = 
                taskAdminController.getAllTasks(pageable);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getData());
        assertEquals(1, response.getBody().getData().getContent().size());
        assertEquals(taskId, response.getBody().getData().getContent().get(0).taskId());
        verify(taskService, times(1)).getAllTasksForAdmin(pageable);
    }

    @Test
    void testGetAllTasks_EmptyPage() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<AdminTaskSummaryResponse> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(taskService.getAllTasksForAdmin(pageable)).thenReturn(emptyPage);

        ResponseEntity<ApiResponse<Page<AdminTaskSummaryResponse>>> response = 
                taskAdminController.getAllTasks(pageable);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody().getData().getContent().size());
        verify(taskService, times(1)).getAllTasksForAdmin(pageable);
    }

    @Test
    void testGetAllTasks_MultiplePages() {
        Pageable pageable = PageRequest.of(1, 10);
        List<AdminTaskSummaryResponse> tasks = List.of(testSummaryResponse);
        Page<AdminTaskSummaryResponse> taskPage = new PageImpl<>(tasks, pageable, 25);

        when(taskService.getAllTasksForAdmin(pageable)).thenReturn(taskPage);

        ResponseEntity<ApiResponse<Page<AdminTaskSummaryResponse>>> response = 
                taskAdminController.getAllTasks(pageable);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(25, response.getBody().getData().getTotalElements());
        assertEquals(3, response.getBody().getData().getTotalPages());
        verify(taskService, times(1)).getAllTasksForAdmin(pageable);
    }

    @Test
    void testGetAllTasks_WithCustomPageSize() {
        Pageable pageable = PageRequest.of(0, 50);
        List<AdminTaskSummaryResponse> tasks = List.of(testSummaryResponse);
        Page<AdminTaskSummaryResponse> taskPage = new PageImpl<>(tasks, pageable, 1);

        when(taskService.getAllTasksForAdmin(pageable)).thenReturn(taskPage);

        ResponseEntity<ApiResponse<Page<AdminTaskSummaryResponse>>> response = 
                taskAdminController.getAllTasks(pageable);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(taskService, times(1)).getAllTasksForAdmin(pageable);
    }

    @Test
    void testGetAllTasks_WithSorting() {
        Pageable pageable = PageRequest.of(0, 20);
        List<AdminTaskSummaryResponse> tasks = List.of(testSummaryResponse);
        Page<AdminTaskSummaryResponse> taskPage = new PageImpl<>(tasks, pageable, 1);

        when(taskService.getAllTasksForAdmin(pageable)).thenReturn(taskPage);

        ResponseEntity<ApiResponse<Page<AdminTaskSummaryResponse>>> response = 
                taskAdminController.getAllTasks(pageable);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(taskService, times(1)).getAllTasksForAdmin(pageable);
    }

    @Test
    void testGetAllTasks_MultipleTasksSummaries() {
        LocalDateTime now = LocalDateTime.now();
        AdminTaskSummaryResponse task1 = new AdminTaskSummaryResponse(
                UUID.randomUUID(),
                "Task 1",
                TaskType.CODING.name(),
                TaskDifficulty.BEGINNER.name(),
                1,
                true,
                now
        );

        AdminTaskSummaryResponse task2 = new AdminTaskSummaryResponse(
                UUID.randomUUID(),
                "Task 2",
                TaskType.ESSAY.name(),
                TaskDifficulty.INTERMEDIATE.name(),
                1,
                true,
                now
        );

        Pageable pageable = PageRequest.of(0, 20);
        List<AdminTaskSummaryResponse> tasks = List.of(task1, task2);
        Page<AdminTaskSummaryResponse> taskPage = new PageImpl<>(tasks, pageable, 2);

        when(taskService.getAllTasksForAdmin(pageable)).thenReturn(taskPage);

        ResponseEntity<ApiResponse<Page<AdminTaskSummaryResponse>>> response = 
                taskAdminController.getAllTasks(pageable);

        assertNotNull(response);
        assertEquals(2, response.getBody().getData().getContent().size());
        verify(taskService, times(1)).getAllTasksForAdmin(pageable);
    }

    @Test
    void testGetAllTasks_ResponseMessage() {
        Pageable pageable = PageRequest.of(0, 20);
        List<AdminTaskSummaryResponse> tasks = List.of(testSummaryResponse);
        Page<AdminTaskSummaryResponse> taskPage = new PageImpl<>(tasks, pageable, 1);

        when(taskService.getAllTasksForAdmin(pageable)).thenReturn(taskPage);

        ResponseEntity<ApiResponse<Page<AdminTaskSummaryResponse>>> response = 
                taskAdminController.getAllTasks(pageable);

        assertEquals("Tasks retrieved successfully", response.getBody().getMessage());
    }

    // ==================== GET TASK BY ID TESTS ====================

    @Test
    void testGetTaskById_Success() {
        when(taskService.getTaskForAdmin(taskId)).thenReturn(testDetailResponse);

        ResponseEntity<ApiResponse<AdminTaskDetailResponse>> response = 
                taskAdminController.getTaskById(taskId);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(taskId, response.getBody().getData().taskId());
        assertEquals("Test Task", response.getBody().getData().title());
        verify(taskService, times(1)).getTaskForAdmin(taskId);
    }

    @Test
    void testGetTaskById_NotFound() {
        when(taskService.getTaskForAdmin(taskId))
                .thenThrow(new ResourceNotFoundException("Task not found"));

        assertThrows(ResourceNotFoundException.class, () -> {
            taskAdminController.getTaskById(taskId);
        });

        verify(taskService, times(1)).getTaskForAdmin(taskId);
    }

    @Test
    void testGetTaskById_ReturnsDetailedInfo() {
        LocalDateTime now = LocalDateTime.now();
        AdminTaskDetailResponse detailWithRewards = new AdminTaskDetailResponse(
                taskId,
                UUID.randomUUID(),
                "Test Task",
                "Test Description",
                "Test Skill",
                TaskType.CODING.name(),
                TaskDifficulty.INTERMEDIATE.name(),
                null,
                1,
                true,
                30,
                100,
                now,
                now
        );

        when(taskService.getTaskForAdmin(taskId)).thenReturn(detailWithRewards);

        ResponseEntity<ApiResponse<AdminTaskDetailResponse>> response = 
                taskAdminController.getTaskById(taskId);

        assertNotNull(response);
        assertEquals(100, response.getBody().getData().xpReward());
        assertEquals(30, response.getBody().getData().estimatedDurationInMinutes());
        assertTrue(response.getBody().getData().isPublished());
    }

    @Test
    void testGetTaskById_ResponseMessage() {
        when(taskService.getTaskForAdmin(taskId)).thenReturn(testDetailResponse);

        ResponseEntity<ApiResponse<AdminTaskDetailResponse>> response = 
                taskAdminController.getTaskById(taskId);

        assertEquals("Task details retrieved successfully", response.getBody().getMessage());
    }

    @Test
    void testGetTaskById_CodingTask() {
        LocalDateTime now = LocalDateTime.now();
        AdminTaskDetailResponse codingTask = new AdminTaskDetailResponse(
                taskId,
                UUID.randomUUID(),
                "Test Task",
                "Test Description",
                "Test Skill",
                TaskType.CODING.name(),
                TaskDifficulty.INTERMEDIATE.name(),
                null,
                1,
                true,
                30,
                100,
                now,
                now
        );
        when(taskService.getTaskForAdmin(taskId)).thenReturn(codingTask);

        ResponseEntity<ApiResponse<AdminTaskDetailResponse>> response = 
                taskAdminController.getTaskById(taskId);

        assertEquals(TaskType.CODING.name(), response.getBody().getData().type());
    }

    @Test
    void testGetTaskById_EssayTask() {
        LocalDateTime now = LocalDateTime.now();
        AdminTaskDetailResponse essayTask = new AdminTaskDetailResponse(
                taskId,
                UUID.randomUUID(),
                "Test Task",
                "Test Description",
                "Test Skill",
                TaskType.ESSAY.name(),
                TaskDifficulty.INTERMEDIATE.name(),
                null,
                1,
                true,
                30,
                100,
                now,
                now
        );
        when(taskService.getTaskForAdmin(taskId)).thenReturn(essayTask);

        ResponseEntity<ApiResponse<AdminTaskDetailResponse>> response = 
                taskAdminController.getTaskById(taskId);

        assertEquals(TaskType.ESSAY.name(), response.getBody().getData().type());
    }

    @Test
    void testGetTaskById_MultipleChoiceTask() {
        LocalDateTime now = LocalDateTime.now();
        AdminTaskDetailResponse mcqTask = new AdminTaskDetailResponse(
                taskId,
                UUID.randomUUID(),
                "Test Task",
                "Test Description",
                "Test Skill",
                TaskType.MULTIPLE_CHOICE.name(),
                TaskDifficulty.INTERMEDIATE.name(),
                null,
                1,
                true,
                30,
                100,
                now,
                now
        );
        when(taskService.getTaskForAdmin(taskId)).thenReturn(mcqTask);

        ResponseEntity<ApiResponse<AdminTaskDetailResponse>> response = 
                taskAdminController.getTaskById(taskId);

        assertEquals(TaskType.MULTIPLE_CHOICE.name(), response.getBody().getData().type());
    }

    @Test
    void testGetTaskById_DifferentDifficulties() {
        LocalDateTime now = LocalDateTime.now();
        AdminTaskDetailResponse advancedTask = new AdminTaskDetailResponse(
                taskId,
                UUID.randomUUID(),
                "Test Task",
                "Test Description",
                "Test Skill",
                TaskType.CODING.name(),
                TaskDifficulty.ADVANCED.name(),
                null,
                1,
                true,
                30,
                100,
                now,
                now
        );
        when(taskService.getTaskForAdmin(taskId)).thenReturn(advancedTask);

        ResponseEntity<ApiResponse<AdminTaskDetailResponse>> response = 
                taskAdminController.getTaskById(taskId);

        assertEquals(TaskDifficulty.ADVANCED.name(), response.getBody().getData().difficulty());
    }

    // ==================== REQUEST TASK GENERATION TESTS ====================

    @Test
    void testRequestTaskGeneration_Success() {
        taskAdminController.requestTaskGeneration(generateRequest, authentication);

        verify(taskService, times(1)).requestSpecificTaskGeneration(generateRequest, adminUserId);
    }

    @Test
    void testRequestTaskGeneration_ReturnsAccepted() {
        ResponseEntity<ApiResponse<Void>> response = 
                taskAdminController.requestTaskGeneration(generateRequest, authentication);

        assertNotNull(response);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertEquals(202, response.getStatusCodeValue());
    }

    @Test
    void testRequestTaskGeneration_ResponseMessage() {
        ResponseEntity<ApiResponse<Void>> response = 
                taskAdminController.requestTaskGeneration(generateRequest, authentication);

        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage().contains("Task generation request accepted"));
    }

    @Test
    void testRequestTaskGeneration_CodingTask() {
        GenerateTaskRequest codingRequest = new GenerateTaskRequest(
                adminUserId,
                TaskType.CODING,
                "JAVASCRIPT",
                TaskDifficulty.BEGINNER,
                "Arrays",
                "JavaScript"
        );

        ResponseEntity<ApiResponse<Void>> response = 
                taskAdminController.requestTaskGeneration(codingRequest, authentication);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        verify(taskService, times(1)).requestSpecificTaskGeneration(codingRequest, adminUserId);
    }

    @Test
    void testRequestTaskGeneration_EssayTask() {
        GenerateTaskRequest essayRequest = new GenerateTaskRequest(
                adminUserId,
                TaskType.ESSAY,
                "PYTHON",
                TaskDifficulty.ADVANCED,
                "Design Patterns",
                "Python"
        );

        ResponseEntity<ApiResponse<Void>> response = 
                taskAdminController.requestTaskGeneration(essayRequest, authentication);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        verify(taskService, times(1)).requestSpecificTaskGeneration(essayRequest, adminUserId);
    }

    @Test
    void testRequestTaskGeneration_DifferentDifficulties() {
        for (TaskDifficulty difficulty : TaskDifficulty.values()) {
            GenerateTaskRequest request = new GenerateTaskRequest(
                    adminUserId,
                    TaskType.CODING,
                    "PYTHON",
                    difficulty,
                    "Test",
                    "Python"
            );

            ResponseEntity<ApiResponse<Void>> response = 
                    taskAdminController.requestTaskGeneration(request, authentication);

            assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        }
        verify(taskService, times(TaskDifficulty.values().length)).requestSpecificTaskGeneration(any(), any());
    }

    @Test
    void testRequestTaskGeneration_WithTopic() {
        GenerateTaskRequest requestWithTopic = new GenerateTaskRequest(
                adminUserId,
                TaskType.CODING,
                "JAVA",
                TaskDifficulty.INTERMEDIATE,
                "Exception Handling",
                "Java"
        );

        ResponseEntity<ApiResponse<Void>> response = 
                taskAdminController.requestTaskGeneration(requestWithTopic, authentication);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        verify(taskService, times(1)).requestSpecificTaskGeneration(requestWithTopic, adminUserId);
    }

    @Test
    void testRequestTaskGeneration_AdminUserIdCaptured() {
        UUID testAdminId = UUID.randomUUID();
        when(authentication.getName()).thenReturn(testAdminId.toString());

        taskAdminController.requestTaskGeneration(generateRequest, authentication);

        verify(taskService, times(1)).requestSpecificTaskGeneration(
                any(GenerateTaskRequest.class),
                eq(testAdminId)
        );
    }

    @Test
    void testRequestTaskGeneration_MultipleRequests() {
        taskAdminController.requestTaskGeneration(generateRequest, authentication);
        taskAdminController.requestTaskGeneration(generateRequest, authentication);
        taskAdminController.requestTaskGeneration(generateRequest, authentication);

        verify(taskService, times(3)).requestSpecificTaskGeneration(any(), any());
    }

    // ==================== RESPONSE STRUCTURE TESTS ====================

    @Test
    void testGetAllTasks_ResponseStructure() {
        Pageable pageable = PageRequest.of(0, 20);
        List<AdminTaskSummaryResponse> tasks = List.of(testSummaryResponse);
        Page<AdminTaskSummaryResponse> taskPage = new PageImpl<>(tasks, pageable, 1);

        when(taskService.getAllTasksForAdmin(pageable)).thenReturn(taskPage);

        ResponseEntity<ApiResponse<Page<AdminTaskSummaryResponse>>> response = 
                taskAdminController.getAllTasks(pageable);

        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getMessage());
        assertNotNull(response.getBody().getData());
    }

    @Test
    void testGetTaskById_ResponseStructure() {
        when(taskService.getTaskForAdmin(taskId)).thenReturn(testDetailResponse);

        ResponseEntity<ApiResponse<AdminTaskDetailResponse>> response = 
                taskAdminController.getTaskById(taskId);

        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getMessage());
        assertNotNull(response.getBody().getData());
    }

    @Test
    void testRequestTaskGeneration_ResponseStructure() {
        ResponseEntity<ApiResponse<Void>> response = 
                taskAdminController.requestTaskGeneration(generateRequest, authentication);

        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getMessage());
    }

    // ==================== PAGINATION TESTS ====================

    @Test
    void testGetAllTasks_FirstPage() {
        LocalDateTime now = LocalDateTime.now();
        Pageable pageable = PageRequest.of(0, 20);
        AdminTaskSummaryResponse task1 = new AdminTaskSummaryResponse(
                UUID.randomUUID(),
                "Task 1",
                TaskType.CODING.name(),
                TaskDifficulty.BEGINNER.name(),
                1,
                true,
                now
        );
        AdminTaskSummaryResponse task2 = new AdminTaskSummaryResponse(
                UUID.randomUUID(),
                "Task 2",
                TaskType.ESSAY.name(),
                TaskDifficulty.INTERMEDIATE.name(),
                1,
                true,
                now
        );

        Page<AdminTaskSummaryResponse> taskPage = new PageImpl<>(
                List.of(task1, task2), pageable, 100
        );

        when(taskService.getAllTasksForAdmin(pageable)).thenReturn(taskPage);

        ResponseEntity<ApiResponse<Page<AdminTaskSummaryResponse>>> response = 
                taskAdminController.getAllTasks(pageable);

        assertTrue(response.getBody().getData().isFirst());
        assertFalse(response.getBody().getData().isLast());
    }

    @Test
    void testGetAllTasks_LastPage() {
        Pageable pageable = PageRequest.of(4, 20);
        List<AdminTaskSummaryResponse> lastPageTasks = List.of(testSummaryResponse);
        Page<AdminTaskSummaryResponse> taskPage = new PageImpl<>(
                lastPageTasks, pageable, 100
        );

        when(taskService.getAllTasksForAdmin(pageable)).thenReturn(taskPage);

        ResponseEntity<ApiResponse<Page<AdminTaskSummaryResponse>>> response = 
                taskAdminController.getAllTasks(pageable);

        assertTrue(response.getBody().getData().isLast());
    }

    @Test
    void testGetAllTasks_MiddlePage() {
        Pageable pageable = PageRequest.of(2, 20);
        Page<AdminTaskSummaryResponse> taskPage = new PageImpl<>(
                List.of(testSummaryResponse), pageable, 100
        );

        when(taskService.getAllTasksForAdmin(pageable)).thenReturn(taskPage);

        ResponseEntity<ApiResponse<Page<AdminTaskSummaryResponse>>> response = 
                taskAdminController.getAllTasks(pageable);

        assertFalse(response.getBody().getData().isFirst());
        assertFalse(response.getBody().getData().isLast());
    }

    // ==================== VARIOUS UUID TESTS ====================

    @Test
    void testGetTaskById_DifferentTaskIds() {
    UUID taskId1 = UUID.randomUUID();
    UUID taskId2 = UUID.randomUUID();
         LocalDateTime now = LocalDateTime.now();

    AdminTaskDetailResponse response1 = new AdminTaskDetailResponse(
                 taskId1,
            UUID.randomUUID(),
            "Task 1",
                 "Description 1",
            "Skill 1",
            TaskType.CODING.name(),
                 TaskDifficulty.BEGINNER.name(),
            null,
    1,
            true,
    30,
                 100,
            now,
            now
        );

         AdminTaskDetailResponse response2 = new AdminTaskDetailResponse(
                 taskId2,
                 UUID.randomUUID(),
                 "Task 2",
                 "Description 2",
                 "Skill 2",
                 TaskType.ESSAY.name(),
                 TaskDifficulty.ADVANCED.name(),
                 null,
                 1,
                 true,
                 45,
                 150,
                 now,
                 now
         );

         when(taskService.getTaskForAdmin(taskId1)).thenReturn(response1);
         when(taskService.getTaskForAdmin(taskId2)).thenReturn(response2);

         ResponseEntity<ApiResponse<AdminTaskDetailResponse>> result1 = 
                 taskAdminController.getTaskById(taskId1);
         ResponseEntity<ApiResponse<AdminTaskDetailResponse>> result2 = 
                 taskAdminController.getTaskById(taskId2);

         assertEquals(taskId1, result1.getBody().getData().taskId());
         assertEquals(taskId2, result2.getBody().getData().taskId());
     }
}
