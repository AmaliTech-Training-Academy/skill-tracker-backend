package com.amalitech.task.service.controller;

import com.amalitech.common.security.dto.response.ApiResponse;
import com.amalitech.task.service.dto.CurrentProgressDTO;
import com.amalitech.task.service.dto.LearningPathDTO;
import com.amalitech.task.service.dto.TaskDTO;
import com.amalitech.task.service.dto.request.UserProfileRequestDTO;
import com.amalitech.task.service.dto.response.LearningPathResponseDTO;
import com.amalitech.task.service.dto.response.UserTasksResponse;
import com.amalitech.task.service.exception.ResourceNotFoundException;
import com.amalitech.task.service.model.enums.TaskDifficulty;
import com.amalitech.task.service.model.enums.TaskType;
import com.amalitech.task.service.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskController Comprehensive Tests")
class TaskControllerComprehensiveTest {

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
        testTaskDTO = buildTestTaskDTO();
    }

    private TaskDTO buildTestTaskDTO() {
        return TaskDTO.builder()
                .id(taskId)
                .title("Test Task")
                .description("Test Description")
                .type(TaskType.CODING)
                .difficulty(TaskDifficulty.INTERMEDIATE)
                .xpReward(100)
                .estimatedDuration(60)
                .skillName("Java")
                .version(1)
                .build();
    }

    private TaskDTO buildTaskDTO(String title, UUID id) {
        return TaskDTO.builder()
                .id(id)
                .title(title)
                .description("Test Description")
                .type(TaskType.CODING)
                .difficulty(TaskDifficulty.INTERMEDIATE)
                .xpReward(100)
                .estimatedDuration(60)
                .skillName("Java")
                .version(1)
                .build();
    }

    @Nested
    @DisplayName("GET /{id} - Get Task By ID")
    class GetTaskByIdTests {

        @Test
        @DisplayName("Should successfully retrieve task by ID")
        void testGetTaskById_Success() {
            when(taskService.getTaskById(taskId)).thenReturn(testTaskDTO);

            ResponseEntity<ApiResponse<TaskDTO>> response = taskController.getTaskById(taskId);

            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(taskId, response.getBody().getData().getId());
            assertEquals("Test Task", response.getBody().getData().getTitle());
            assertTrue(response.getBody().isSuccess());
            verify(taskService, times(1)).getTaskById(taskId);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when task not found")
        void testGetTaskById_NotFound() {
            when(taskService.getTaskById(taskId))
                    .thenThrow(new ResourceNotFoundException("Task not found"));

            assertThrows(ResourceNotFoundException.class, () -> {
                taskController.getTaskById(taskId);
            });

            verify(taskService, times(1)).getTaskById(taskId);
        }

        @Test
        @DisplayName("Should return correct response message")
        void testGetTaskById_ResponseMessage() {
            when(taskService.getTaskById(taskId)).thenReturn(testTaskDTO);

            ResponseEntity<ApiResponse<TaskDTO>> response = taskController.getTaskById(taskId);

            assertEquals("Task retrieved successfully.", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Should return task with all fields populated")
        void testGetTaskById_AllFieldsPopulated() {
            when(taskService.getTaskById(taskId)).thenReturn(testTaskDTO);

            ResponseEntity<ApiResponse<TaskDTO>> response = taskController.getTaskById(taskId);

            TaskDTO data = response.getBody().getData();
            assertEquals("Test Task", data.getTitle());
            assertEquals("Test Description", data.getDescription());
            assertEquals(TaskType.CODING, data.getType());
            assertEquals(TaskDifficulty.INTERMEDIATE, data.getDifficulty());
            assertEquals(100, data.getXpReward());
            assertEquals(60, data.getEstimatedDuration());
            assertEquals("Java", data.getSkillName());
            assertEquals(1, data.getVersion());
        }

        @Test
        @DisplayName("Should handle different TaskTypes")
        void testGetTaskById_DifferentTaskTypes() {
            TaskDTO mcqTask = TaskDTO.builder()
                    .id(taskId)
                    .title("Test Task")
                    .description("Test Description")
                    .type(TaskType.MULTIPLE_CHOICE)
                    .difficulty(TaskDifficulty.INTERMEDIATE)
                    .xpReward(100)
                    .estimatedDuration(60)
                    .skillName("Java")
                    .version(1)
                    .build();

            when(taskService.getTaskById(taskId)).thenReturn(mcqTask);

            ResponseEntity<ApiResponse<TaskDTO>> response = taskController.getTaskById(taskId);

            assertEquals(TaskType.MULTIPLE_CHOICE, response.getBody().getData().getType());
        }

        @Test
        @DisplayName("Should handle different TaskDifficulties")
        void testGetTaskById_DifferentDifficulties() {
            TaskDTO easyTask = TaskDTO.builder()
                    .id(taskId)
                    .title("Test Task")
                    .description("Test Description")
                    .type(TaskType.CODING)
                    .difficulty(TaskDifficulty.BEGINNER)
                    .xpReward(100)
                    .estimatedDuration(60)
                    .skillName("Java")
                    .version(1)
                    .build();

            when(taskService.getTaskById(taskId)).thenReturn(easyTask);

            ResponseEntity<ApiResponse<TaskDTO>> response = taskController.getTaskById(taskId);

            assertEquals(TaskDifficulty.BEGINNER, response.getBody().getData().getDifficulty());
        }

        @Test
        @DisplayName("Should verify service is called with correct UUID")
        void testGetTaskById_VerifyServiceCall() {
            UUID specificId = UUID.randomUUID();
            when(taskService.getTaskById(specificId)).thenReturn(testTaskDTO);

            taskController.getTaskById(specificId);

            verify(taskService).getTaskById(specificId);
        }
    }

    @Nested
    @DisplayName("POST /generate/learning-path - Generate Learning Path")
    class GenerateLearningPathTests {

        @Test
        @DisplayName("Should successfully generate learning path")
        void testGenerateLearningPath_Success() throws Exception {
            CurrentProgressDTO currentProgress = CurrentProgressDTO.builder()
                    .build();

            UserProfileRequestDTO requestDTO = UserProfileRequestDTO.builder()
                    .userId(userId)
                    .career_goals(List.of("Backend Developer", "DevOps"))
                    .current_progress(currentProgress)
                    .build();

            LearningPathDTO learningPath = LearningPathDTO.builder()
                    .build();

            LearningPathResponseDTO responseDTO = LearningPathResponseDTO.builder()
                    .learningPath(learningPath)
                    .build();

            when(taskService.generateLearningPath(requestDTO)).thenReturn(responseDTO);

            ResponseEntity<ApiResponse<LearningPathResponseDTO>> response = 
                    taskController.generateLearningPath(requestDTO);

            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().isSuccess());
            assertEquals("Learning path Generated Successfully", response.getBody().getMessage());
            verify(taskService, times(1)).generateLearningPath(requestDTO);
        }

        @Test
        @DisplayName("Should handle multiple career goals")
        void testGenerateLearningPath_MultipleCareerGoals() throws Exception {
            CurrentProgressDTO currentProgress = CurrentProgressDTO.builder()
                    .build();

            List<String> goals = List.of("Full Stack Developer", "Cloud Architect", "ML Engineer");

            UserProfileRequestDTO requestDTO = UserProfileRequestDTO.builder()
                    .userId(userId)
                    .career_goals(goals)
                    .current_progress(currentProgress)
                    .build();

            LearningPathDTO learningPath = LearningPathDTO.builder()
                    .build();

            LearningPathResponseDTO responseDTO = LearningPathResponseDTO.builder()
                    .learningPath(learningPath)
                    .build();

            when(taskService.generateLearningPath(requestDTO)).thenReturn(responseDTO);

            ResponseEntity<ApiResponse<LearningPathResponseDTO>> response = 
                    taskController.generateLearningPath(requestDTO);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(taskService).generateLearningPath(argThat(req -> 
                    req.getCareer_goals().size() == 3));
        }

        @Test
        @DisplayName("Should wrap response in ApiResponse")
        void testGenerateLearningPath_WrapsResponse() throws Exception {
            CurrentProgressDTO currentProgress = CurrentProgressDTO.builder()
                    .build();

            UserProfileRequestDTO requestDTO = UserProfileRequestDTO.builder()
                    .userId(userId)
                    .career_goals(List.of("QA Engineer"))
                    .current_progress(currentProgress)
                    .build();

            LearningPathDTO learningPath = LearningPathDTO.builder()
                    .build();

            LearningPathResponseDTO responseDTO = LearningPathResponseDTO.builder()
                    .learningPath(learningPath)
                    .build();

            when(taskService.generateLearningPath(requestDTO)).thenReturn(responseDTO);

            ResponseEntity<ApiResponse<LearningPathResponseDTO>> response = 
                    taskController.generateLearningPath(requestDTO);

            assertNotNull(response.getBody().getData());
            assertNotNull(response.getBody().getData().getLearningPath());
        }

        @Test
        @DisplayName("Should return 200 OK status")
        void testGenerateLearningPath_StatusOk() throws Exception {
            CurrentProgressDTO currentProgress = CurrentProgressDTO.builder()
                    .build();

            UserProfileRequestDTO requestDTO = UserProfileRequestDTO.builder()
                    .userId(userId)
                    .career_goals(List.of("Blockchain Developer"))
                    .current_progress(currentProgress)
                    .build();

            LearningPathDTO learningPath = LearningPathDTO.builder()
                    .build();

            LearningPathResponseDTO responseDTO = LearningPathResponseDTO.builder()
                    .learningPath(learningPath)
                    .build();

            when(taskService.generateLearningPath(requestDTO)).thenReturn(responseDTO);

            ResponseEntity<ApiResponse<LearningPathResponseDTO>> response = 
                    taskController.generateLearningPath(requestDTO);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("Should pass request to service")
        void testGenerateLearningPath_PassRequestToService() throws Exception {
            CurrentProgressDTO currentProgress = CurrentProgressDTO.builder()
                    .build();

            UserProfileRequestDTO requestDTO = UserProfileRequestDTO.builder()
                    .userId(userId)
                    .career_goals(List.of("Data Scientist"))
                    .current_progress(currentProgress)
                    .build();

            LearningPathDTO learningPath = LearningPathDTO.builder()
                    .build();

            LearningPathResponseDTO responseDTO = LearningPathResponseDTO.builder()
                    .learningPath(learningPath)
                    .build();

            when(taskService.generateLearningPath(requestDTO)).thenReturn(responseDTO);

            taskController.generateLearningPath(requestDTO);

            verify(taskService).generateLearningPath(requestDTO);
        }
    }

    @Nested
    @DisplayName("GET / - Get Personalized Tasks")
    class GetPersonalizedTasksTests {

        @Test
        @DisplayName("Should retrieve personalized tasks successfully")
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
        @DisplayName("Should use default limit when not specified")
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
        @DisplayName("Should handle empty result set")
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
        @DisplayName("Should handle multiple results")
        void testGetPersonalizedTasks_MultipleResults() {
            TaskDTO task1 = buildTaskDTO("Task 1", UUID.randomUUID());
            TaskDTO task2 = buildTaskDTO("Task 2", UUID.randomUUID());
            TaskDTO task3 = buildTaskDTO("Task 3", UUID.randomUUID());

            List<TaskDTO> tasks = List.of(task1, task2, task3);
            when(authentication.getName()).thenReturn(userId.toString());
            when(taskService.getPersonalizedTasks(userId, "PYTHON", TaskType.CODING, 10))
                    .thenReturn(tasks);

            ResponseEntity<ApiResponse<List<TaskDTO>>> response = taskController.getPersonalizedTasks(
                    "PYTHON", TaskType.CODING, 10, authentication
            );

            assertNotNull(response);
            assertEquals(3, response.getBody().getData().size());
            assertEquals("Task 1", response.getBody().getData().get(0).getTitle());
            assertEquals("Task 2", response.getBody().getData().get(1).getTitle());
            assertEquals("Task 3", response.getBody().getData().get(2).getTitle());
        }

        @Test
        @DisplayName("Should extract userId from authentication")
        void testGetPersonalizedTasks_ExtractUserId() {
            UUID specificUserId = UUID.randomUUID();
            when(authentication.getName()).thenReturn(specificUserId.toString());
            when(taskService.getPersonalizedTasks(specificUserId, "JAVA", TaskType.MULTIPLE_CHOICE, 5))
                    .thenReturn(List.of());

            taskController.getPersonalizedTasks("JAVA", TaskType.MULTIPLE_CHOICE, 5, authentication);

            verify(taskService).getPersonalizedTasks(
                    argThat(uuid -> uuid.equals(specificUserId)),
                    eq("JAVA"),
                    eq(TaskType.MULTIPLE_CHOICE),
                    eq(5)
            );
        }

        @Test
        @DisplayName("Should handle different task types")
        void testGetPersonalizedTasks_DifferentTaskTypes() {
            List<TaskDTO> tasks = List.of(testTaskDTO);
            when(authentication.getName()).thenReturn(userId.toString());

            taskController.getPersonalizedTasks("SKILL", TaskType.ESSAY, 5, authentication);

            verify(taskService).getPersonalizedTasks(userId, "SKILL", TaskType.ESSAY, 5);
        }

        @Test
        @DisplayName("Should handle custom limit values")
        void testGetPersonalizedTasks_CustomLimit() {
            when(authentication.getName()).thenReturn(userId.toString());
            when(taskService.getPersonalizedTasks(userId, "PYTHON", TaskType.CODING, 20))
                    .thenReturn(List.of());

            taskController.getPersonalizedTasks("PYTHON", TaskType.CODING, 20, authentication);

            verify(taskService).getPersonalizedTasks(userId, "PYTHON", TaskType.CODING, 20);
        }

        @Test
        @DisplayName("Should return success response message")
        void testGetPersonalizedTasks_ResponseMessage() {
            when(authentication.getName()).thenReturn(userId.toString());
            when(taskService.getPersonalizedTasks(userId, "PYTHON", TaskType.CODING, 5))
                    .thenReturn(List.of(testTaskDTO));

            ResponseEntity<ApiResponse<List<TaskDTO>>> response = taskController.getPersonalizedTasks(
                    "PYTHON", TaskType.CODING, 5, authentication
            );

            assertEquals("Tasks retrieved successfully", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("GET /my-tasks - Get User Tasks")
    class GetUserTasksTests {

        @Test
        @DisplayName("Should retrieve user tasks with default pagination")
        void testGetUserTasks_DefaultPagination() {
            PageImpl<TaskDTO> pendingPage = new PageImpl<>(List.of(testTaskDTO), 
                    PageRequest.of(0, 10), 1);
            PageImpl<TaskDTO> completedPage = new PageImpl<>(List.of(), 
                    PageRequest.of(0, 10), 0);

            UserTasksResponse userTasksResponse = new UserTasksResponse(pendingPage, completedPage);

            when(authentication.getName()).thenReturn(userId.toString());
            when(taskService.getUserTasksGroupedByStatus(userId, 0, 10, 0, 10, null, "ALL_PERIODS"))
                    .thenReturn(userTasksResponse);

            ResponseEntity<ApiResponse<UserTasksResponse>> response = 
                    taskController.getUserTasks(0, 10, 0, 10, null, "ALL_PERIODS", authentication);

            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Tasks retrieved successfully", response.getBody().getMessage());
            assertEquals(1, response.getBody().getData().pending().getTotalElements());
            assertEquals(0, response.getBody().getData().completed().getTotalElements());
            verify(taskService, times(1)).getUserTasksGroupedByStatus(userId, 0, 10, 0, 10, null, "ALL_PERIODS");
        }

        @Test
        @DisplayName("Should handle custom pagination parameters")
        void testGetUserTasks_CustomPagination() {
            List<TaskDTO> pendingTasks = createTaskList(20, "Pending Task");
            List<TaskDTO> completedTasks = createTaskList(15, "Completed Task");

            PageImpl<TaskDTO> pendingPage = new PageImpl<>(pendingTasks, 
                    PageRequest.of(1, 20), 40);
            PageImpl<TaskDTO> completedPage = new PageImpl<>(completedTasks, 
                    PageRequest.of(2, 15), 45);

            UserTasksResponse userTasksResponse = new UserTasksResponse(pendingPage, completedPage);

            when(authentication.getName()).thenReturn(userId.toString());
            when(taskService.getUserTasksGroupedByStatus(userId, 1, 20, 2, 15, null, "ALL_PERIODS"))
                    .thenReturn(userTasksResponse);

            ResponseEntity<ApiResponse<UserTasksResponse>> response = 
                    taskController.getUserTasks(1, 20, 2, 15, null, "ALL_PERIODS", authentication);

            assertEquals(40, response.getBody().getData().pending().getTotalElements());
            assertEquals(45, response.getBody().getData().completed().getTotalElements());
            verify(taskService, times(1)).getUserTasksGroupedByStatus(userId, 1, 20, 2, 15, null, "ALL_PERIODS");
        }

        @Test
        @DisplayName("Should extract userId from authentication")
        void testGetUserTasks_ExtractsUserIdFromAuthentication() {
            UUID specificUserId = UUID.randomUUID();
            PageImpl<TaskDTO> emptyPendingPage = new PageImpl<>(List.of(), 
                    PageRequest.of(0, 10), 0);
            PageImpl<TaskDTO> emptyCompletedPage = new PageImpl<>(List.of(), 
                    PageRequest.of(0, 10), 0);

            UserTasksResponse userTasksResponse = new UserTasksResponse(emptyPendingPage, emptyCompletedPage);

            when(authentication.getName()).thenReturn(specificUserId.toString());
            when(taskService.getUserTasksGroupedByStatus(specificUserId, 0, 10, 0, 10, null, "ALL_PERIODS"))
                    .thenReturn(userTasksResponse);

            taskController.getUserTasks(0, 10, 0, 10, null, "ALL_PERIODS", authentication);

            verify(taskService, times(1)).getUserTasksGroupedByStatus(
                    argThat(uuid -> uuid.equals(specificUserId)), 
                    eq(0), eq(10), eq(0), eq(10), eq(null), eq("ALL_PERIODS")
            );
        }

        @Test
        @DisplayName("Should wrap response in ApiResponse")
        void testGetUserTasks_ResponseWrappedInApiResponse() {
            PageImpl<TaskDTO> pendingPage = new PageImpl<>(List.of(testTaskDTO), 
                    PageRequest.of(0, 10), 1);
            PageImpl<TaskDTO> completedPage = new PageImpl<>(List.of(), 
                    PageRequest.of(0, 10), 0);

            UserTasksResponse userTasksResponse = new UserTasksResponse(pendingPage, completedPage);

            when(authentication.getName()).thenReturn(userId.toString());
            when(taskService.getUserTasksGroupedByStatus(userId, 0, 10, 0, 10, null, "ALL_PERIODS"))
                    .thenReturn(userTasksResponse);

            ResponseEntity<ApiResponse<UserTasksResponse>> response = 
                    taskController.getUserTasks(0, 10, 0, 10, null, "ALL_PERIODS", authentication);

            assertNotNull(response.getBody());
            assertNotNull(response.getBody().getData());
            assertTrue(response.getBody().isSuccess());
        }

        @Test
        @DisplayName("Should handle no tasks found scenario")
        void testGetUserTasks_NoTasksFound() {
            PageImpl<TaskDTO> emptyPendingPage = new PageImpl<>(List.of(), 
                    PageRequest.of(0, 10), 0);
            PageImpl<TaskDTO> emptyCompletedPage = new PageImpl<>(List.of(), 
                    PageRequest.of(0, 10), 0);

            UserTasksResponse userTasksResponse = new UserTasksResponse(emptyPendingPage, emptyCompletedPage);

            when(authentication.getName()).thenReturn(userId.toString());
            when(taskService.getUserTasksGroupedByStatus(userId, 0, 10, 0, 10, null, "ALL_PERIODS"))
                    .thenReturn(userTasksResponse);

            ResponseEntity<ApiResponse<UserTasksResponse>> response = 
                    taskController.getUserTasks(0, 10, 0, 10, null, "ALL_PERIODS", authentication);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(0, response.getBody().getData().pending().getTotalElements());
            assertEquals(0, response.getBody().getData().completed().getTotalElements());
        }

        @Test
        @DisplayName("Should handle only pending tasks scenario")
        void testGetUserTasks_OnlyPendingTasks() {
            TaskDTO pendingTask = buildTaskDTO("Pending Task", UUID.randomUUID());

            PageImpl<TaskDTO> pendingPage = new PageImpl<>(List.of(pendingTask), 
                    PageRequest.of(0, 10), 5);
            PageImpl<TaskDTO> emptyCompletedPage = new PageImpl<>(List.of(), 
                    PageRequest.of(0, 10), 0);

            UserTasksResponse userTasksResponse = new UserTasksResponse(pendingPage, emptyCompletedPage);

            when(authentication.getName()).thenReturn(userId.toString());
            when(taskService.getUserTasksGroupedByStatus(userId, 0, 10, 0, 10, null, "ALL_PERIODS"))
                    .thenReturn(userTasksResponse);

            ResponseEntity<ApiResponse<UserTasksResponse>> response = 
                    taskController.getUserTasks(0, 10, 0, 10, null, "ALL_PERIODS", authentication);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(response.getBody().getData().pending().getTotalElements() > 0);
            assertEquals(0, response.getBody().getData().completed().getTotalElements());
        }

        @Test
        @DisplayName("Should handle only completed tasks scenario")
        void testGetUserTasks_OnlyCompletedTasks() {
            TaskDTO completedTask = buildTaskDTO("Completed Task", UUID.randomUUID());

            PageImpl<TaskDTO> emptyPendingPage = new PageImpl<>(List.of(), 
                    PageRequest.of(0, 10), 0);
            PageImpl<TaskDTO> completedPage = new PageImpl<>(List.of(completedTask), 
                    PageRequest.of(0, 10), 3);

            UserTasksResponse userTasksResponse = new UserTasksResponse(emptyPendingPage, completedPage);

            when(authentication.getName()).thenReturn(userId.toString());
            when(taskService.getUserTasksGroupedByStatus(userId, 0, 10, 0, 10, null, "ALL_PERIODS"))
                    .thenReturn(userTasksResponse);

            ResponseEntity<ApiResponse<UserTasksResponse>> response = 
                    taskController.getUserTasks(0, 10, 0, 10, null, "ALL_PERIODS", authentication);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(0, response.getBody().getData().pending().getTotalElements());
            assertTrue(response.getBody().getData().completed().getTotalElements() > 0);
        }

        @Test
        @DisplayName("Should handle both pending and completed tasks")
        void testGetUserTasks_BothPendingAndCompletedTasks() {
            TaskDTO pendingTask = buildTaskDTO("Pending Task", UUID.randomUUID());
            TaskDTO completedTask = buildTaskDTO("Completed Task", UUID.randomUUID());

            PageImpl<TaskDTO> pendingPage = new PageImpl<>(List.of(pendingTask), 
                    PageRequest.of(0, 10), 1);
            PageImpl<TaskDTO> completedPage = new PageImpl<>(List.of(completedTask), 
                    PageRequest.of(0, 10), 1);

            UserTasksResponse userTasksResponse = new UserTasksResponse(pendingPage, completedPage);

            when(authentication.getName()).thenReturn(userId.toString());
            when(taskService.getUserTasksGroupedByStatus(userId, 0, 10, 0, 10, null, "ALL_PERIODS"))
                    .thenReturn(userTasksResponse);

            ResponseEntity<ApiResponse<UserTasksResponse>> response = 
                    taskController.getUserTasks(0, 10, 0, 10, null, "ALL_PERIODS", authentication);

            assertNotNull(response.getBody().getData().pending());
            assertNotNull(response.getBody().getData().completed());
            assertEquals(1, response.getBody().getData().pending().getTotalElements());
            assertEquals(1, response.getBody().getData().completed().getTotalElements());
        }

        @Test
        @DisplayName("Should handle multiple tasks in both pages")
        void testGetUserTasks_MultipleTasksInBothPages() {
            List<TaskDTO> pendingTasks = createTaskList(10, "Pending");
            List<TaskDTO> completedTasks = createTaskList(10, "Completed");

            PageImpl<TaskDTO> pendingPage = new PageImpl<>(pendingTasks, 
                    PageRequest.of(0, 10), 15);
            PageImpl<TaskDTO> completedPage = new PageImpl<>(completedTasks, 
                    PageRequest.of(0, 10), 10);

            UserTasksResponse userTasksResponse = new UserTasksResponse(pendingPage, completedPage);

            when(authentication.getName()).thenReturn(userId.toString());
            when(taskService.getUserTasksGroupedByStatus(userId, 0, 10, 0, 10, null, "ALL_PERIODS"))
                    .thenReturn(userTasksResponse);

            ResponseEntity<ApiResponse<UserTasksResponse>> response = 
                    taskController.getUserTasks(0, 10, 0, 10, null, "ALL_PERIODS", authentication);

            assertEquals(10, response.getBody().getData().pending().getContent().size());
            assertEquals(10, response.getBody().getData().completed().getContent().size());
            assertEquals(15, response.getBody().getData().pending().getTotalElements());
            assertEquals(10, response.getBody().getData().completed().getTotalElements());
        }

        @Test
        @DisplayName("Should return correct response message")
        void testGetUserTasks_ResponseMessage() {
            PageImpl<TaskDTO> pendingPage = new PageImpl<>(List.of(testTaskDTO), 
                    PageRequest.of(0, 10), 1);
            PageImpl<TaskDTO> completedPage = new PageImpl<>(List.of(), 
                    PageRequest.of(0, 10), 0);

            UserTasksResponse userTasksResponse = new UserTasksResponse(pendingPage, completedPage);

            when(authentication.getName()).thenReturn(userId.toString());
            when(taskService.getUserTasksGroupedByStatus(userId, 0, 10, 0, 10, null, "ALL_PERIODS"))
                    .thenReturn(userTasksResponse);

            ResponseEntity<ApiResponse<UserTasksResponse>> response = 
                    taskController.getUserTasks(0, 10, 0, 10, null, "ALL_PERIODS", authentication);

            assertEquals("Tasks retrieved successfully", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Should handle large page sizes")
        void testGetUserTasks_LargePageSize() {
            List<TaskDTO> pendingTasks = createTaskList(50, "Pending");

            PageImpl<TaskDTO> pendingPage = new PageImpl<>(pendingTasks, 
                    PageRequest.of(0, 50), 100);
            PageImpl<TaskDTO> completedPage = new PageImpl<>(List.of(), 
                    PageRequest.of(0, 10), 0);

            UserTasksResponse userTasksResponse = new UserTasksResponse(pendingPage, completedPage);

            when(authentication.getName()).thenReturn(userId.toString());
            when(taskService.getUserTasksGroupedByStatus(userId, 0, 50, 0, 10, null, "ALL_PERIODS"))
                    .thenReturn(userTasksResponse);

            ResponseEntity<ApiResponse<UserTasksResponse>> response = 
                    taskController.getUserTasks(0, 50, 0, 10, null, "ALL_PERIODS", authentication);

            assertEquals(50, response.getBody().getData().pending().getContent().size());
            assertEquals(100, response.getBody().getData().pending().getTotalElements());
        }

        @Test
        @DisplayName("Should handle independent pagination for pending and completed")
        void testGetUserTasks_IndependentPagination() {
            List<TaskDTO> pendingTasks = createTaskList(25, "Pending");
            List<TaskDTO> completedTasks = createTaskList(15, "Completed");

            PageImpl<TaskDTO> pendingPage = new PageImpl<>(pendingTasks, 
                    PageRequest.of(2, 25), 75);
            PageImpl<TaskDTO> completedPage = new PageImpl<>(completedTasks, 
                    PageRequest.of(3, 15), 60);

            UserTasksResponse userTasksResponse = new UserTasksResponse(pendingPage, completedPage);

            when(authentication.getName()).thenReturn(userId.toString());
            when(taskService.getUserTasksGroupedByStatus(userId, 2, 25, 3, 15, null, "ALL_PERIODS"))
                    .thenReturn(userTasksResponse);

            ResponseEntity<ApiResponse<UserTasksResponse>> response = 
                    taskController.getUserTasks(2, 25, 3, 15, null, "ALL_PERIODS", authentication);

            verify(taskService).getUserTasksGroupedByStatus(userId, 2, 25, 3, 15, null, "ALL_PERIODS");
            assertEquals(75, response.getBody().getData().pending().getTotalElements());
            assertEquals(60, response.getBody().getData().completed().getTotalElements());
        }

        @Test
        @DisplayName("Should return OK status")
        void testGetUserTasks_StatusCode() {
            PageImpl<TaskDTO> pendingPage = new PageImpl<>(List.of(testTaskDTO), 
                    PageRequest.of(0, 10), 1);
            PageImpl<TaskDTO> completedPage = new PageImpl<>(List.of(), 
                    PageRequest.of(0, 10), 0);

            UserTasksResponse userTasksResponse = new UserTasksResponse(pendingPage, completedPage);

            when(authentication.getName()).thenReturn(userId.toString());
            when(taskService.getUserTasksGroupedByStatus(userId, 0, 10, 0, 10, null, "ALL_PERIODS"))
                    .thenReturn(userTasksResponse);

            ResponseEntity<ApiResponse<UserTasksResponse>> response = 
                    taskController.getUserTasks(0, 10, 0, 10, null, "ALL_PERIODS", authentication);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("Should filter by skill name when provided")
        void testGetUserTasks_FilterBySkillName() {
            TaskDTO pythonTask = buildTaskDTO("Python Task", UUID.randomUUID());
            pythonTask.setSkillName("Python");

            PageImpl<TaskDTO> pendingPage = new PageImpl<>(List.of(pythonTask), 
                    PageRequest.of(0, 10), 1);
            PageImpl<TaskDTO> completedPage = new PageImpl<>(List.of(), 
                    PageRequest.of(0, 10), 0);

            UserTasksResponse userTasksResponse = new UserTasksResponse(pendingPage, completedPage);

            when(authentication.getName()).thenReturn(userId.toString());
            when(taskService.getUserTasksGroupedByStatus(userId, 0, 10, 0, 10, "Python", "ALL_PERIODS"))
                    .thenReturn(userTasksResponse);

            ResponseEntity<ApiResponse<UserTasksResponse>> response = 
                    taskController.getUserTasks(0, 10, 0, 10, "Python", "ALL_PERIODS", authentication);

            verify(taskService).getUserTasksGroupedByStatus(userId, 0, 10, 0, 10, "Python", "ALL_PERIODS");
            assertEquals(1, response.getBody().getData().pending().getTotalElements());
        }

        @Test
        @DisplayName("Should handle multiple skill names (different calls)")
        void testGetUserTasks_DifferentSkills() {
            TaskDTO javaTask = buildTaskDTO("Java Task", UUID.randomUUID());
            javaTask.setSkillName("Java");

            PageImpl<TaskDTO> pendingPage = new PageImpl<>(List.of(javaTask), 
                    PageRequest.of(0, 10), 1);
            PageImpl<TaskDTO> completedPage = new PageImpl<>(List.of(), 
                    PageRequest.of(0, 10), 0);

            UserTasksResponse userTasksResponse = new UserTasksResponse(pendingPage, completedPage);

            when(authentication.getName()).thenReturn(userId.toString());
            when(taskService.getUserTasksGroupedByStatus(userId, 0, 10, 0, 10, "Java", "ALL_PERIODS"))
                    .thenReturn(userTasksResponse);

            taskController.getUserTasks(0, 10, 0, 10, "Java", "ALL_PERIODS", authentication);

            verify(taskService).getUserTasksGroupedByStatus(userId, 0, 10, 0, 10, "Java", "ALL_PERIODS");
        }

        @Test
        @DisplayName("Should handle completed period filter TODAY")
        void testGetUserTasks_FilterByCompletedPeriod_Today() {
            PageImpl<TaskDTO> pendingPage = new PageImpl<>(List.of(testTaskDTO), 
                    PageRequest.of(0, 10), 1);
            PageImpl<TaskDTO> completedPage = new PageImpl<>(List.of(testTaskDTO), 
                    PageRequest.of(0, 10), 1);

            UserTasksResponse userTasksResponse = new UserTasksResponse(pendingPage, completedPage);

            when(authentication.getName()).thenReturn(userId.toString());
            when(taskService.getUserTasksGroupedByStatus(userId, 0, 10, 0, 10, null, "TODAY"))
                    .thenReturn(userTasksResponse);

            ResponseEntity<ApiResponse<UserTasksResponse>> response = 
                    taskController.getUserTasks(0, 10, 0, 10, null, "TODAY", authentication);

            verify(taskService).getUserTasksGroupedByStatus(userId, 0, 10, 0, 10, null, "TODAY");
            assertEquals(1, response.getBody().getData().completed().getTotalElements());
        }

        @Test
        @DisplayName("Should handle completed period filter LAST_7_DAYS")
        void testGetUserTasks_FilterByCompletedPeriod_Last7Days() {
            List<TaskDTO> completedTasks = createTaskList(5, "Completed");

            PageImpl<TaskDTO> pendingPage = new PageImpl<>(List.of(testTaskDTO), 
                    PageRequest.of(0, 10), 1);
            PageImpl<TaskDTO> completedPage = new PageImpl<>(completedTasks, 
                    PageRequest.of(0, 10), 5);

            UserTasksResponse userTasksResponse = new UserTasksResponse(pendingPage, completedPage);

            when(authentication.getName()).thenReturn(userId.toString());
            when(taskService.getUserTasksGroupedByStatus(userId, 0, 10, 0, 10, null, "LAST_7_DAYS"))
                    .thenReturn(userTasksResponse);

            ResponseEntity<ApiResponse<UserTasksResponse>> response = 
                    taskController.getUserTasks(0, 10, 0, 10, null, "LAST_7_DAYS", authentication);

            verify(taskService).getUserTasksGroupedByStatus(userId, 0, 10, 0, 10, null, "LAST_7_DAYS");
            assertEquals(5, response.getBody().getData().completed().getTotalElements());
        }

        @Test
        @DisplayName("Should handle completed period filter LAST_30_DAYS")
        void testGetUserTasks_FilterByCompletedPeriod_Last30Days() {
            List<TaskDTO> completedTasks = createTaskList(10, "Completed");

            PageImpl<TaskDTO> pendingPage = new PageImpl<>(List.of(), 
                    PageRequest.of(0, 10), 0);
            PageImpl<TaskDTO> completedPage = new PageImpl<>(completedTasks, 
                    PageRequest.of(0, 10), 10);

            UserTasksResponse userTasksResponse = new UserTasksResponse(pendingPage, completedPage);

            when(authentication.getName()).thenReturn(userId.toString());
            when(taskService.getUserTasksGroupedByStatus(userId, 0, 10, 0, 10, null, "LAST_30_DAYS"))
                    .thenReturn(userTasksResponse);

            ResponseEntity<ApiResponse<UserTasksResponse>> response = 
                    taskController.getUserTasks(0, 10, 0, 10, null, "LAST_30_DAYS", authentication);

            verify(taskService).getUserTasksGroupedByStatus(userId, 0, 10, 0, 10, null, "LAST_30_DAYS");
            assertEquals(10, response.getBody().getData().completed().getTotalElements());
        }

        @Test
        @DisplayName("Should handle skill filter with completed period filter")
        void testGetUserTasks_SkillAndPeriodFilters() {
            List<TaskDTO> pendingTasks = createTaskList(3, "Pending Task");
            List<TaskDTO> completedTasks = createTaskList(2, "Completed Task");

            PageImpl<TaskDTO> pendingPage = new PageImpl<>(pendingTasks, 
                    PageRequest.of(0, 10), 3);
            PageImpl<TaskDTO> completedPage = new PageImpl<>(completedTasks, 
                    PageRequest.of(0, 10), 2);

            UserTasksResponse userTasksResponse = new UserTasksResponse(pendingPage, completedPage);

            when(authentication.getName()).thenReturn(userId.toString());
            when(taskService.getUserTasksGroupedByStatus(userId, 0, 10, 0, 10, "Python", "LAST_7_DAYS"))
                    .thenReturn(userTasksResponse);

            ResponseEntity<ApiResponse<UserTasksResponse>> response = 
                    taskController.getUserTasks(0, 10, 0, 10, "Python", "LAST_7_DAYS", authentication);

            verify(taskService).getUserTasksGroupedByStatus(userId, 0, 10, 0, 10, "Python", "LAST_7_DAYS");
            assertEquals(3, response.getBody().getData().pending().getTotalElements());
            assertEquals(2, response.getBody().getData().completed().getTotalElements());
        }

        @Test
        @DisplayName("Should handle page index and size independently")
        void testGetUserTasks_IndependentPageSizes() {
            List<TaskDTO> pendingTasks = createTaskList(30, "Pending");
            List<TaskDTO> completedTasks = createTaskList(20, "Completed");

            PageImpl<TaskDTO> pendingPage = new PageImpl<>(pendingTasks, 
                    PageRequest.of(3, 30), 150);
            PageImpl<TaskDTO> completedPage = new PageImpl<>(completedTasks, 
                    PageRequest.of(1, 20), 100);

            UserTasksResponse userTasksResponse = new UserTasksResponse(pendingPage, completedPage);

            when(authentication.getName()).thenReturn(userId.toString());
            when(taskService.getUserTasksGroupedByStatus(userId, 3, 30, 1, 20, null, "ALL_PERIODS"))
                    .thenReturn(userTasksResponse);

            ResponseEntity<ApiResponse<UserTasksResponse>> response = 
                    taskController.getUserTasks(3, 30, 1, 20, null, "ALL_PERIODS", authentication);

            verify(taskService).getUserTasksGroupedByStatus(userId, 3, 30, 1, 20, null, "ALL_PERIODS");
            assertEquals(150, response.getBody().getData().pending().getTotalElements());
            assertEquals(100, response.getBody().getData().completed().getTotalElements());
        }

        @Test
        @DisplayName("Should maintain pagination even with zero results")
        void testGetUserTasks_PaginationWithZeroResults() {
            PageImpl<TaskDTO> emptyPendingPage = new PageImpl<>(List.of(), 
                    PageRequest.of(5, 10), 0);
            PageImpl<TaskDTO> emptyCompletedPage = new PageImpl<>(List.of(), 
                    PageRequest.of(5, 10), 0);

            UserTasksResponse userTasksResponse = new UserTasksResponse(emptyPendingPage, emptyCompletedPage);

            when(authentication.getName()).thenReturn(userId.toString());
            when(taskService.getUserTasksGroupedByStatus(userId, 5, 10, 5, 10, null, "ALL_PERIODS"))
                    .thenReturn(userTasksResponse);

            ResponseEntity<ApiResponse<UserTasksResponse>> response = 
                    taskController.getUserTasks(5, 10, 5, 10, null, "ALL_PERIODS", authentication);

            assertEquals(0, response.getBody().getData().pending().getTotalElements());
            assertEquals(0, response.getBody().getData().completed().getTotalElements());
            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("Should handle authentication name conversion to UUID")
        void testGetUserTasks_AuthenticationNameConversion() {
            UUID expectedUserId = UUID.randomUUID();
            PageImpl<TaskDTO> pendingPage = new PageImpl<>(List.of(), 
                    PageRequest.of(0, 10), 0);
            PageImpl<TaskDTO> completedPage = new PageImpl<>(List.of(), 
                    PageRequest.of(0, 10), 0);

            UserTasksResponse userTasksResponse = new UserTasksResponse(pendingPage, completedPage);

            when(authentication.getName()).thenReturn(expectedUserId.toString());
            when(taskService.getUserTasksGroupedByStatus(expectedUserId, 0, 10, 0, 10, null, "ALL_PERIODS"))
                    .thenReturn(userTasksResponse);

            taskController.getUserTasks(0, 10, 0, 10, null, "ALL_PERIODS", authentication);

            verify(authentication).getName();
            verify(taskService).getUserTasksGroupedByStatus(expectedUserId, 0, 10, 0, 10, null, "ALL_PERIODS");
        }

        @Test
        @DisplayName("Should correctly pass all parameters to service")
        void testGetUserTasks_AllParametersPassed() {
            PageImpl<TaskDTO> pendingPage = new PageImpl<>(List.of(), 
                    PageRequest.of(0, 10), 0);
            PageImpl<TaskDTO> completedPage = new PageImpl<>(List.of(), 
                    PageRequest.of(0, 10), 0);

            UserTasksResponse userTasksResponse = new UserTasksResponse(pendingPage, completedPage);

            String skillName = "JavaScript";
            String period = "LAST_7_DAYS";
            int pendingPage_param = 1;
            int pendingSize_param = 15;
            int completedPage_param = 2;
            int completedSize_param = 20;

            when(authentication.getName()).thenReturn(userId.toString());
            when(taskService.getUserTasksGroupedByStatus(userId, pendingPage_param, pendingSize_param, 
                    completedPage_param, completedSize_param, skillName, period))
                    .thenReturn(userTasksResponse);

            ResponseEntity<ApiResponse<UserTasksResponse>> response = 
                    taskController.getUserTasks(pendingPage_param, pendingSize_param, 
                            completedPage_param, completedSize_param, skillName, period, authentication);

            verify(taskService).getUserTasksGroupedByStatus(
                    userId, pendingPage_param, pendingSize_param, 
                    completedPage_param, completedSize_param, skillName, period
            );
        }

        @Test
        @DisplayName("Should handle null skill parameter")
        void testGetUserTasks_NullSkillParameter() {
            PageImpl<TaskDTO> pendingPage = new PageImpl<>(List.of(testTaskDTO), 
                    PageRequest.of(0, 10), 1);
            PageImpl<TaskDTO> completedPage = new PageImpl<>(List.of(), 
                    PageRequest.of(0, 10), 0);

            UserTasksResponse userTasksResponse = new UserTasksResponse(pendingPage, completedPage);

            when(authentication.getName()).thenReturn(userId.toString());
            when(taskService.getUserTasksGroupedByStatus(userId, 0, 10, 0, 10, null, "ALL_PERIODS"))
                    .thenReturn(userTasksResponse);

            ResponseEntity<ApiResponse<UserTasksResponse>> response = 
                    taskController.getUserTasks(0, 10, 0, 10, null, "ALL_PERIODS", authentication);

            verify(taskService).getUserTasksGroupedByStatus(userId, 0, 10, 0, 10, null, "ALL_PERIODS");
        }

        @Test
        @DisplayName("Should handle minimum page sizes")
        void testGetUserTasks_MinimumPageSizes() {
            TaskDTO task = buildTaskDTO("Task", UUID.randomUUID());

            PageImpl<TaskDTO> pendingPage = new PageImpl<>(List.of(task), 
                    PageRequest.of(0, 1), 5);
            PageImpl<TaskDTO> completedPage = new PageImpl<>(List.of(task), 
                    PageRequest.of(0, 1), 3);

            UserTasksResponse userTasksResponse = new UserTasksResponse(pendingPage, completedPage);

            when(authentication.getName()).thenReturn(userId.toString());
            when(taskService.getUserTasksGroupedByStatus(userId, 0, 1, 0, 1, null, "ALL_PERIODS"))
                    .thenReturn(userTasksResponse);

            ResponseEntity<ApiResponse<UserTasksResponse>> response = 
                    taskController.getUserTasks(0, 1, 0, 1, null, "ALL_PERIODS", authentication);

            assertEquals(1, response.getBody().getData().pending().getContent().size());
            assertEquals(1, response.getBody().getData().completed().getContent().size());
        }

        @Test
        @DisplayName("Should verify service called exactly once")
        void testGetUserTasks_ServiceCalledOnce() {
            PageImpl<TaskDTO> pendingPage = new PageImpl<>(List.of(), 
                    PageRequest.of(0, 10), 0);
            PageImpl<TaskDTO> completedPage = new PageImpl<>(List.of(), 
                    PageRequest.of(0, 10), 0);

            UserTasksResponse userTasksResponse = new UserTasksResponse(pendingPage, completedPage);

            when(authentication.getName()).thenReturn(userId.toString());
            when(taskService.getUserTasksGroupedByStatus(userId, 0, 10, 0, 10, null, "ALL_PERIODS"))
                    .thenReturn(userTasksResponse);

            taskController.getUserTasks(0, 10, 0, 10, null, "ALL_PERIODS", authentication);

            verify(taskService, times(1)).getUserTasksGroupedByStatus(userId, 0, 10, 0, 10, null, "ALL_PERIODS");
        }

        @Test
        @DisplayName("Should return data wrapped correctly in response")
        void testGetUserTasks_ResponseDataWrapped() {
            PageImpl<TaskDTO> pendingPage = new PageImpl<>(List.of(testTaskDTO), 
                    PageRequest.of(0, 10), 1);
            PageImpl<TaskDTO> completedPage = new PageImpl<>(List.of(testTaskDTO), 
                    PageRequest.of(0, 10), 1);

            UserTasksResponse userTasksResponse = new UserTasksResponse(pendingPage, completedPage);

            when(authentication.getName()).thenReturn(userId.toString());
            when(taskService.getUserTasksGroupedByStatus(userId, 0, 10, 0, 10, null, "ALL_PERIODS"))
                    .thenReturn(userTasksResponse);

            ResponseEntity<ApiResponse<UserTasksResponse>> response = 
                    taskController.getUserTasks(0, 10, 0, 10, null, "ALL_PERIODS", authentication);

            assertNotNull(response.getBody().getData());
            assertNotNull(response.getBody().getData().pending());
            assertNotNull(response.getBody().getData().completed());
            assertTrue(response.getBody().isSuccess());
            assertEquals("Tasks retrieved successfully", response.getBody().getMessage());
        }
    }

    private List<TaskDTO> createTaskList(int count, String titlePrefix) {
        List<TaskDTO> tasks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            tasks.add(buildTaskDTO(titlePrefix + " " + i, UUID.randomUUID()));
        }
        return tasks;
    }
}
