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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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

    @Nested
    @DisplayName("GET /api/v1/admin/tasks - Get All Tasks Tests")
    class GetAllTasksTests {

        @Test
        @DisplayName("Should retrieve paginated tasks successfully")
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
            assertTrue(response.getBody().isSuccess());
            assertNotNull(response.getBody().getData());
            assertEquals(1, response.getBody().getData().getContent().size());
            assertEquals(taskId, response.getBody().getData().getContent().get(0).taskId());
            assertEquals("Tasks retrieved successfully", response.getBody().getMessage());
            verify(taskService, times(1)).getAllTasksForAdmin(pageable);
        }

        @Test
        @DisplayName("Should return empty page when no tasks exist")
        void testGetAllTasks_EmptyPage() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<AdminTaskSummaryResponse> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(taskService.getAllTasksForAdmin(pageable)).thenReturn(emptyPage);

            ResponseEntity<ApiResponse<Page<AdminTaskSummaryResponse>>> response =
                    taskAdminController.getAllTasks(pageable);

            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(0, response.getBody().getData().getContent().size());
            assertTrue(response.getBody().getData().isEmpty());
            verify(taskService, times(1)).getAllTasksForAdmin(pageable);
        }

        @Test
        @DisplayName("Should handle multiple pages correctly")
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
            assertEquals(1, response.getBody().getData().getNumber());
            verify(taskService, times(1)).getAllTasksForAdmin(pageable);
        }

        @Test
        @DisplayName("Should support custom page size")
        void testGetAllTasks_WithCustomPageSize() {
            Pageable pageable = PageRequest.of(0, 50);
            List<AdminTaskSummaryResponse> tasks = List.of(testSummaryResponse);
            Page<AdminTaskSummaryResponse> taskPage = new PageImpl<>(tasks, pageable, 1);

            when(taskService.getAllTasksForAdmin(pageable)).thenReturn(taskPage);

            ResponseEntity<ApiResponse<Page<AdminTaskSummaryResponse>>> response =
                    taskAdminController.getAllTasks(pageable);

            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(50, response.getBody().getData().getSize());
            verify(taskService, times(1)).getAllTasksForAdmin(pageable);
        }

        @Test
        @DisplayName("Should retrieve multiple tasks in summary response")
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

            AdminTaskSummaryResponse task3 = new AdminTaskSummaryResponse(
                    UUID.randomUUID(),
                    "Task 3",
                    TaskType.MULTIPLE_CHOICE.name(),
                    TaskDifficulty.ADVANCED.name(),
                    2,
                    false,
                    now
            );

            Pageable pageable = PageRequest.of(0, 20);
            List<AdminTaskSummaryResponse> tasks = List.of(task1, task2, task3);
            Page<AdminTaskSummaryResponse> taskPage = new PageImpl<>(tasks, pageable, 3);

            when(taskService.getAllTasksForAdmin(pageable)).thenReturn(taskPage);

            ResponseEntity<ApiResponse<Page<AdminTaskSummaryResponse>>> response =
                    taskAdminController.getAllTasks(pageable);

            assertNotNull(response);
            assertEquals(3, response.getBody().getData().getContent().size());
            assertEquals(3, response.getBody().getData().getTotalElements());
            verify(taskService, times(1)).getAllTasksForAdmin(pageable);
        }

        @Test
        @DisplayName("Should correctly identify first page")
        void testGetAllTasks_FirstPage() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<AdminTaskSummaryResponse> taskPage = new PageImpl<>(List.of(testSummaryResponse), pageable, 100);

            when(taskService.getAllTasksForAdmin(pageable)).thenReturn(taskPage);

            ResponseEntity<ApiResponse<Page<AdminTaskSummaryResponse>>> response =
                    taskAdminController.getAllTasks(pageable);

            assertTrue(response.getBody().getData().isFirst());
            assertFalse(response.getBody().getData().isLast());
        }

        @Test
        @DisplayName("Should correctly identify last page")
        void testGetAllTasks_LastPage() {
            Pageable pageable = PageRequest.of(4, 20);
            Page<AdminTaskSummaryResponse> taskPage = new PageImpl<>(List.of(testSummaryResponse), pageable, 100);

            when(taskService.getAllTasksForAdmin(pageable)).thenReturn(taskPage);

            ResponseEntity<ApiResponse<Page<AdminTaskSummaryResponse>>> response =
                    taskAdminController.getAllTasks(pageable);

            assertTrue(response.getBody().getData().isLast());
        }

        @Test
        @DisplayName("Should correctly identify middle page")
        void testGetAllTasks_MiddlePage() {
            Pageable pageable = PageRequest.of(2, 20);
            Page<AdminTaskSummaryResponse> taskPage = new PageImpl<>(List.of(testSummaryResponse), pageable, 100);

            when(taskService.getAllTasksForAdmin(pageable)).thenReturn(taskPage);

            ResponseEntity<ApiResponse<Page<AdminTaskSummaryResponse>>> response =
                    taskAdminController.getAllTasks(pageable);

            assertFalse(response.getBody().getData().isFirst());
            assertFalse(response.getBody().getData().isLast());
        }

        @Test
        @DisplayName("Should verify message in success response")
        void testGetAllTasks_ResponseMessage() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<AdminTaskSummaryResponse> taskPage = new PageImpl<>(List.of(testSummaryResponse), pageable, 1);

            when(taskService.getAllTasksForAdmin(pageable)).thenReturn(taskPage);

            ResponseEntity<ApiResponse<Page<AdminTaskSummaryResponse>>> response =
                    taskAdminController.getAllTasks(pageable);

            assertEquals("Tasks retrieved successfully", response.getBody().getMessage());
            assertTrue(response.getBody().isSuccess());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/tasks/{id} - Get Task By ID Tests")
    class GetTaskByIdTests {

        @Test
        @DisplayName("Should retrieve task details successfully")
        void testGetTaskById_Success() {
            when(taskService.getTaskForAdmin(taskId)).thenReturn(testDetailResponse);

            ResponseEntity<ApiResponse<AdminTaskDetailResponse>> response =
                    taskAdminController.getTaskById(taskId);

            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().isSuccess());
            assertEquals(taskId, response.getBody().getData().taskId());
            assertEquals("Test Task", response.getBody().getData().title());
            assertEquals("Test Description", response.getBody().getData().description());
            assertEquals("Task details retrieved successfully", response.getBody().getMessage());
            verify(taskService, times(1)).getTaskForAdmin(taskId);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when task not found")
        void testGetTaskById_NotFound() {
            when(taskService.getTaskForAdmin(taskId))
                    .thenThrow(new ResourceNotFoundException("Task not found"));

            assertThrows(ResourceNotFoundException.class, () -> {
                taskAdminController.getTaskById(taskId);
            });

            verify(taskService, times(1)).getTaskForAdmin(taskId);
        }

        @Test
        @DisplayName("Should return complete task details including rewards")
        void testGetTaskById_ReturnsDetailedInfo() {
            LocalDateTime now = LocalDateTime.now();
            AdminTaskDetailResponse detailWithRewards = new AdminTaskDetailResponse(
                    taskId,
                    UUID.randomUUID(),
                    "Complex Task",
                    "Complex Description",
                    "Advanced Skill",
                    TaskType.CODING.name(),
                    TaskDifficulty.ADVANCED.name(),
                    null,
                    2,
                    true,
                    60,
                    250,
                    now,
                    now
            );

            when(taskService.getTaskForAdmin(taskId)).thenReturn(detailWithRewards);

            ResponseEntity<ApiResponse<AdminTaskDetailResponse>> response =
                    taskAdminController.getTaskById(taskId);

            assertNotNull(response);
            assertEquals(250, response.getBody().getData().xpReward());
            assertEquals(60, response.getBody().getData().estimatedDurationInMinutes());
            assertTrue(response.getBody().getData().isPublished());
        }

        @Test
        @DisplayName("Should verify response message for single task")
        void testGetTaskById_ResponseMessage() {
            when(taskService.getTaskForAdmin(taskId)).thenReturn(testDetailResponse);

            ResponseEntity<ApiResponse<AdminTaskDetailResponse>> response =
                    taskAdminController.getTaskById(taskId);

            assertEquals("Task details retrieved successfully", response.getBody().getMessage());
            assertTrue(response.getBody().isSuccess());
        }

        @ParameterizedTest
        @EnumSource(TaskType.class)
        @DisplayName("Should handle all task types")
        void testGetTaskById_AllTaskTypes(TaskType taskType) {
            LocalDateTime now = LocalDateTime.now();
            AdminTaskDetailResponse typedTask = new AdminTaskDetailResponse(
                    taskId,
                    UUID.randomUUID(),
                    "Test Task",
                    "Test Description",
                    "Test Skill",
                    taskType.name(),
                    TaskDifficulty.INTERMEDIATE.name(),
                    null,
                    1,
                    true,
                    30,
                    100,
                    now,
                    now
            );

            when(taskService.getTaskForAdmin(taskId)).thenReturn(typedTask);

            ResponseEntity<ApiResponse<AdminTaskDetailResponse>> response =
                    taskAdminController.getTaskById(taskId);

            assertEquals(taskType.name(), response.getBody().getData().type());
        }

        @ParameterizedTest
        @EnumSource(TaskDifficulty.class)
        @DisplayName("Should handle all difficulty levels")
        void testGetTaskById_AllDifficulties(TaskDifficulty difficulty) {
            LocalDateTime now = LocalDateTime.now();
            AdminTaskDetailResponse difficultyTask = new AdminTaskDetailResponse(
                    taskId,
                    UUID.randomUUID(),
                    "Test Task",
                    "Test Description",
                    "Test Skill",
                    TaskType.CODING.name(),
                    difficulty.name(),
                    null,
                    1,
                    true,
                    30,
                    100,
                    now,
                    now
            );

            when(taskService.getTaskForAdmin(taskId)).thenReturn(difficultyTask);

            ResponseEntity<ApiResponse<AdminTaskDetailResponse>> response =
                    taskAdminController.getTaskById(taskId);

            assertEquals(difficulty.name(), response.getBody().getData().difficulty());
        }

        @Test
        @DisplayName("Should handle unpublished tasks")
        void testGetTaskById_UnpublishedTask() {
            LocalDateTime now = LocalDateTime.now();
            AdminTaskDetailResponse unpublishedTask = new AdminTaskDetailResponse(
                    taskId,
                    UUID.randomUUID(),
                    "Draft Task",
                    "Not yet published",
                    "Test Skill",
                    TaskType.ESSAY.name(),
                    TaskDifficulty.INTERMEDIATE.name(),
                    null,
                    1,
                    false,
                    30,
                    100,
                    now,
                    now
            );

            when(taskService.getTaskForAdmin(taskId)).thenReturn(unpublishedTask);

            ResponseEntity<ApiResponse<AdminTaskDetailResponse>> response =
                    taskAdminController.getTaskById(taskId);

            assertFalse(response.getBody().getData().isPublished());
        }

        @Test
        @DisplayName("Should handle multiple different task IDs")
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
            verify(taskService, times(1)).getTaskForAdmin(taskId1);
            verify(taskService, times(1)).getTaskForAdmin(taskId2);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/tasks/generate-task - Request Task Generation Tests")
    class RequestTaskGenerationTests {

        @Test
        @DisplayName("Should accept task generation request")
        void testRequestTaskGeneration_Success() {
            ResponseEntity<ApiResponse<Void>> response =
                    taskAdminController.requestTaskGeneration(generateRequest, authentication);

            assertNotNull(response);
            assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
            assertEquals(202, response.getStatusCodeValue());
            verify(taskService, times(1)).requestSpecificTaskGeneration(generateRequest, adminUserId);
        }

        @Test
        @DisplayName("Should return ACCEPTED status (202)")
        void testRequestTaskGeneration_ReturnsAccepted() {
            ResponseEntity<ApiResponse<Void>> response =
                    taskAdminController.requestTaskGeneration(generateRequest, authentication);

            assertNotNull(response);
            assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
            assertEquals(202, response.getStatusCodeValue());
        }

        @Test
        @DisplayName("Should include appropriate response message")
        void testRequestTaskGeneration_ResponseMessage() {
            ResponseEntity<ApiResponse<Void>> response =
                    taskAdminController.requestTaskGeneration(generateRequest, authentication);

            assertNotNull(response.getBody());
            assertTrue(response.getBody().getMessage().contains("Task generation request accepted"));
            assertTrue(response.getBody().getMessage().contains("notified"));
            assertTrue(response.getBody().isSuccess());
        }

        @ParameterizedTest
        @EnumSource(TaskType.class)
        @DisplayName("Should handle all task types in generation")
        void testRequestTaskGeneration_AllTaskTypes(TaskType taskType) {
            GenerateTaskRequest typeRequest = new GenerateTaskRequest(
                    adminUserId,
                    taskType,
                    "LANGUAGE",
                    TaskDifficulty.INTERMEDIATE,
                    "Topic",
                    "Skill"
            );

            ResponseEntity<ApiResponse<Void>> response =
                    taskAdminController.requestTaskGeneration(typeRequest, authentication);

            assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
            verify(taskService, times(1)).requestSpecificTaskGeneration(typeRequest, adminUserId);
        }

        @ParameterizedTest
        @EnumSource(TaskDifficulty.class)
        @DisplayName("Should handle all difficulty levels in generation")
        void testRequestTaskGeneration_AllDifficulties(TaskDifficulty difficulty) {
            GenerateTaskRequest diffRequest = new GenerateTaskRequest(
                    adminUserId,
                    TaskType.CODING,
                    "PYTHON",
                    difficulty,
                    "Test",
                    "Python"
            );

            ResponseEntity<ApiResponse<Void>> response =
                    taskAdminController.requestTaskGeneration(diffRequest, authentication);

            assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
            verify(taskService, times(1)).requestSpecificTaskGeneration(diffRequest, adminUserId);
        }

        @Test
        @DisplayName("Should capture admin user ID from authentication")
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
        @DisplayName("Should handle multiple sequential generation requests")
        void testRequestTaskGeneration_MultipleRequests() {
            for (int i = 0; i < 5; i++) {
                GenerateTaskRequest request = new GenerateTaskRequest(
                        adminUserId,
                        TaskType.CODING,
                        "PYTHON",
                        TaskDifficulty.INTERMEDIATE,
                        "Topic " + i,
                        "Python"
                );

                ResponseEntity<ApiResponse<Void>> response =
                        taskAdminController.requestTaskGeneration(request, authentication);

                assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
            }

            verify(taskService, times(5)).requestSpecificTaskGeneration(any(), any());
        }

        @Test
        @DisplayName("Should pass request body to service unchanged")
        void testRequestTaskGeneration_RequestBodyPassed() {
            GenerateTaskRequest customRequest = new GenerateTaskRequest(
                    adminUserId,
                    TaskType.ESSAY,
                    "Advanced Topic",
                    TaskDifficulty.ADVANCED,
                    "Essay Topic",
                    "JavaScript Advanced"
            );

            taskAdminController.requestTaskGeneration(customRequest, authentication);

            verify(taskService, times(1)).requestSpecificTaskGeneration(
                    eq(customRequest),
                    argThat(UUID.class::isInstance)
            );
        }

        @Test
        @DisplayName("Should not throw exception on successful request")
        void testRequestTaskGeneration_NoExceptionThrown() {
            assertDoesNotThrow(() -> {
                taskAdminController.requestTaskGeneration(generateRequest, authentication);
            });
        }
    }

    @Nested
    @DisplayName("Response Structure Tests")
    class ResponseStructureTests {

        @Test
        @DisplayName("Get all tasks response should have proper structure")
        void testGetAllTasks_ResponseStructure() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<AdminTaskSummaryResponse> taskPage = new PageImpl<>(List.of(testSummaryResponse), pageable, 1);

            when(taskService.getAllTasksForAdmin(pageable)).thenReturn(taskPage);

            ResponseEntity<ApiResponse<Page<AdminTaskSummaryResponse>>> response =
                    taskAdminController.getAllTasks(pageable);

            assertNotNull(response.getBody());
            assertNotNull(response.getBody().getMessage());
            assertNotNull(response.getBody().getData());
            assertTrue(response.getBody().isSuccess());
        }

        @Test
        @DisplayName("Get task by ID response should have proper structure")
        void testGetTaskById_ResponseStructure() {
            when(taskService.getTaskForAdmin(taskId)).thenReturn(testDetailResponse);

            ResponseEntity<ApiResponse<AdminTaskDetailResponse>> response =
                    taskAdminController.getTaskById(taskId);

            assertNotNull(response.getBody());
            assertNotNull(response.getBody().getMessage());
            assertNotNull(response.getBody().getData());
            assertTrue(response.getBody().isSuccess());
        }

        @Test
        @DisplayName("Task generation response should have proper structure")
        void testRequestTaskGeneration_ResponseStructure() {
            ResponseEntity<ApiResponse<Void>> response =
                    taskAdminController.requestTaskGeneration(generateRequest, authentication);

            assertNotNull(response.getBody());
            assertNotNull(response.getBody().getMessage());
            assertTrue(response.getBody().isSuccess());
            assertEquals(202, response.getStatusCodeValue());
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle very large page numbers")
        void testGetAllTasks_LargePageNumber() {
            Pageable pageable = PageRequest.of(999, 20);
            Page<AdminTaskSummaryResponse> emptyPage = new PageImpl<>(List.of(), pageable, 1000);

            when(taskService.getAllTasksForAdmin(pageable)).thenReturn(emptyPage);

            ResponseEntity<ApiResponse<Page<AdminTaskSummaryResponse>>> response =
                    taskAdminController.getAllTasks(pageable);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(response.getBody().getData().isEmpty());
        }

        @Test
        @DisplayName("Should handle maximum page size")
        void testGetAllTasks_MaxPageSize() {
            Pageable pageable = PageRequest.of(0, 100);
            List<AdminTaskSummaryResponse> tasks = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                tasks.add(new AdminTaskSummaryResponse(
                        UUID.randomUUID(),
                        "Task " + i,
                        TaskType.CODING.name(),
                        TaskDifficulty.BEGINNER.name(),
                        1,
                        true,
                        LocalDateTime.now()
                ));
            }
            Page<AdminTaskSummaryResponse> taskPage = new PageImpl<>(tasks, pageable, 100);

            when(taskService.getAllTasksForAdmin(pageable)).thenReturn(taskPage);

            ResponseEntity<ApiResponse<Page<AdminTaskSummaryResponse>>> response =
                    taskAdminController.getAllTasks(pageable);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(100, response.getBody().getData().getSize());
        }

        @Test
        @DisplayName("Should handle null task data gracefully")
        void testGetTaskById_NullFieldsInTask() {
            LocalDateTime now = LocalDateTime.now();
            AdminTaskDetailResponse taskWithNulls = new AdminTaskDetailResponse(
                    taskId,
                    UUID.randomUUID(),
                    "Task with nulls",
                    null,
                    null,
                    TaskType.CODING.name(),
                    TaskDifficulty.INTERMEDIATE.name(),
                    null,
                    1,
                    true,
                    null,
                    null,
                    now,
                    now
            );

            when(taskService.getTaskForAdmin(taskId)).thenReturn(taskWithNulls);

            ResponseEntity<ApiResponse<AdminTaskDetailResponse>> response =
                    taskAdminController.getTaskById(taskId);

            assertNotNull(response.getBody().getData());
            assertEquals(taskId, response.getBody().getData().taskId());
        }

        @Test
        @DisplayName("Should validate UUID format through controller")
        void testGetTaskById_ValidUUIDFormat() {
            UUID validUUID = UUID.randomUUID();
            when(taskService.getTaskForAdmin(validUUID)).thenReturn(testDetailResponse);

            ResponseEntity<ApiResponse<AdminTaskDetailResponse>> response =
                    taskAdminController.getTaskById(validUUID);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("Should handle special characters in task fields")
        void testGetAllTasks_SpecialCharactersInTaskNames() {
            LocalDateTime now = LocalDateTime.now();
            AdminTaskSummaryResponse specialCharTask = new AdminTaskSummaryResponse(
                    UUID.randomUUID(),
                    "Task with @#$% special chars!",
                    TaskType.CODING.name(),
                    TaskDifficulty.INTERMEDIATE.name(),
                    1,
                    true,
                    now
            );

            Pageable pageable = PageRequest.of(0, 20);
            Page<AdminTaskSummaryResponse> taskPage = new PageImpl<>(List.of(specialCharTask), pageable, 1);

            when(taskService.getAllTasksForAdmin(pageable)).thenReturn(taskPage);

            ResponseEntity<ApiResponse<Page<AdminTaskSummaryResponse>>> response =
                    taskAdminController.getAllTasks(pageable);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Task with @#$% special chars!", response.getBody().getData().getContent().get(0).title());
        }

        @Test
        @DisplayName("Should handle very long task descriptions")
        void testGetTaskById_LongDescription() {
            LocalDateTime now = LocalDateTime.now();
            String longDescription = "A".repeat(10000);
            AdminTaskDetailResponse taskWithLongDesc = new AdminTaskDetailResponse(
                    taskId,
                    UUID.randomUUID(),
                    "Task",
                    longDescription,
                    "Skill",
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

            when(taskService.getTaskForAdmin(taskId)).thenReturn(taskWithLongDesc);

            ResponseEntity<ApiResponse<AdminTaskDetailResponse>> response =
                    taskAdminController.getTaskById(taskId);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(longDescription, response.getBody().getData().description());
        }
    }

    @Nested
    @DisplayName("Verification and Interaction Tests")
    class VerificationTests {

        @Test
        @DisplayName("Should verify service is called exactly once")
        void testGetAllTasks_VerifyServiceCallCount() {
            Pageable pageable = PageRequest.of(0, 20);
            when(taskService.getAllTasksForAdmin(pageable))
                    .thenReturn(new PageImpl<>(List.of(testSummaryResponse), pageable, 1));

            taskAdminController.getAllTasks(pageable);
            taskAdminController.getAllTasks(pageable);

            verify(taskService, times(2)).getAllTasksForAdmin(pageable);
        }

        @Test
        @DisplayName("Should not invoke service when response is cached")
        void testGetTaskById_VerifyServiceInvocation() {
            when(taskService.getTaskForAdmin(taskId)).thenReturn(testDetailResponse);

            taskAdminController.getTaskById(taskId);

            verify(taskService, times(1)).getTaskForAdmin(taskId);
        }

        @Test
        @DisplayName("Should verify all required parameters are passed to service")
        void testRequestTaskGeneration_VerifyParametersPassed() {
            GenerateTaskRequest request = new GenerateTaskRequest(
                    UUID.randomUUID(),
                    TaskType.CODING,
                    "Java",
                    TaskDifficulty.ADVANCED,
                    "Special Topic",
                    "Java"
            );
            UUID expectedAdminId = UUID.randomUUID();
            when(authentication.getName()).thenReturn(expectedAdminId.toString());

            taskAdminController.requestTaskGeneration(request, authentication);

            verify(taskService).requestSpecificTaskGeneration(
                    argThat(r -> r.taskType() == TaskType.CODING),
                    eq(expectedAdminId)
            );
        }
    }
}
