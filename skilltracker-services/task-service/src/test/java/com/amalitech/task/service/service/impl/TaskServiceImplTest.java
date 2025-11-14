package com.amalitech.task.service.service.impl;

import com.amalitech.task.service.dto.TaskAvailabilityDTO;
import com.amalitech.task.service.dto.TaskDTO;
import com.amalitech.task.service.dto.request.BatchGenerationRequest;
import com.amalitech.task.service.exception.ResourceNotFoundException;
import com.amalitech.task.service.mapper.TaskMapper;
import com.amalitech.task.service.model.Task;
import com.amalitech.task.service.model.TaskSubmission;
import com.amalitech.task.service.model.UserSkillProfile;
import com.amalitech.task.service.model.enums.SubmissionStatus;
import com.amalitech.task.service.model.enums.TaskDifficulty;
import com.amalitech.task.service.model.enums.TaskType;
import com.amalitech.task.service.model.view.SkillView;
import com.amalitech.task.service.repository.TaskRepository;
import com.amalitech.task.service.repository.TaskSubmissionRepository;
import com.amalitech.task.service.repository.UserSkillProfileRepository;
import com.amalitech.task.service.service.SkillService;
import com.amalitech.task.service.events.RabbitMQEventProducer;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import com.amalitech.task.service.dto.response.AdminTaskDetailResponse;
import com.amalitech.task.service.dto.response.AdminTaskSummaryResponse;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock(lenient = true)
    private TaskRepository taskRepository;

    @Mock(lenient = true)
    private UserSkillProfileRepository userSkillProfileRepository;

    @Mock(lenient = true)
    private SkillService skillService;

    @Mock(lenient = true)
    private TaskSubmissionRepository submissionRepository;

    @Mock(lenient = true)
    private RabbitMQEventProducer taskEventProducer;

    @Mock(lenient = true)
    private TaskMapper taskMapper;

    @Mock(lenient = true)
    private StringRedisTemplate redisTemplate;

    @Mock(lenient = true)
    private ValueOperations<String, String> redisOps;

    @InjectMocks
    private TaskServiceImpl taskService;

    private UUID userId;
    private UUID skillId;
    private UUID taskId;
    private Task testTask;
    private TaskDTO testTaskDTO;
    private SkillView testSkill;
    private UserSkillProfile userSkillProfile;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        skillId = UUID.randomUUID();
        taskId = UUID.randomUUID();

        testSkill = new SkillView();
        testSkill.setId(skillId);
        testSkill.setName("PYTHON");

        testTask = Task.builder()
                .id(taskId)
                .title("Test Task")
                .description("Test Description")
                .type(TaskType.CODING)
                .difficulty(TaskDifficulty.BEGINNER)
                .build();

        testTaskDTO = new TaskDTO();
        testTaskDTO.setId(taskId);
        testTaskDTO.setTitle("Test Task");

        UserSkillProfile.UserSkillId id = new UserSkillProfile.UserSkillId(userId, skillId);
        userSkillProfile = UserSkillProfile.builder()
                .id(id)
                .skillName("PYTHON")
                .difficulty(TaskDifficulty.BEGINNER)
                .build();

        when(redisTemplate.opsForValue()).thenReturn(redisOps);
        ReflectionTestUtils.setField(taskService, "minTasksPerDifficulty", 5);
    }

    private TaskSubmission createTaskSubmission(Task task) {
        TaskSubmission submission = new TaskSubmission();
        submission.setId(UUID.randomUUID());
        submission.setUserId(userId);
        submission.setTask(task);
        submission.setIsCorrect(true);
        submission.setStatus(SubmissionStatus.COMPLETED);
        submission.setSubmittedAt(LocalDateTime.now());
        return submission;
    }

    @Test
    void testGetTaskById_Success() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(testTask));
        when(taskMapper.toDTO(testTask)).thenReturn(testTaskDTO);

        TaskDTO result = taskService.getTaskById(taskId);

        assertNotNull(result);
        assertEquals(taskId, result.getId());
        verify(taskRepository, times(1)).findById(taskId);
        verify(taskMapper, times(1)).toDTO(testTask);
    }

    @Test
    void testGetTaskById_NotFound() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            taskService.getTaskById(taskId);
        });

        verify(taskRepository, times(1)).findById(taskId);
    }

    @Test
    void testGetPersonalizedTasks_Success() {
        List<Task> tasks = List.of(testTask);

        when(userSkillProfileRepository.findByIdUserIdAndSkillName(userId, "PYTHON"))
                .thenReturn(Optional.of(userSkillProfile));
        when(taskRepository.findBySkillIdAndDifficultyAndType(
                any(), any(TaskDifficulty.class), any(TaskType.class), anyBoolean(), any(Pageable.class)))
                .thenReturn(tasks);
        when(redisOps.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);
        when(taskMapper.toDTO(any(Task.class))).thenReturn(testTaskDTO);

        List<TaskDTO> result = taskService.getPersonalizedTasks(userId, "PYTHON", TaskType.CODING, 5);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(userSkillProfileRepository, times(1)).findByIdUserIdAndSkillName(userId, "PYTHON");
    }

    @Test
    void testGetPersonalizedTasks_UserSkillProfileNotFound() {
        when(userSkillProfileRepository.findByIdUserIdAndSkillName(userId, "UNKNOWN"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            taskService.getPersonalizedTasks(userId, "UNKNOWN", TaskType.CODING, 5);
        });

        verify(userSkillProfileRepository, times(1)).findByIdUserIdAndSkillName(userId, "UNKNOWN");
    }

    @Test
    void testGetTasksForSkillAndDifficulty_Success() {
        List<Task> tasks = List.of(testTask);
        List<TaskDTO> taskDTOs = List.of(testTaskDTO);

        when(skillService.getSkillByName("PYTHON")).thenReturn(testSkill);
        when(taskRepository.findBySkillIdAndDifficultyAndType(
                skillId, TaskDifficulty.BEGINNER, TaskType.CODING, true, PageRequest.of(0, 5)))
                .thenReturn(tasks);
        when(redisOps.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);
        when(taskMapper.toDTO(testTask)).thenReturn(testTaskDTO);

        List<TaskDTO> result = taskService.getTasksForSkillAndDifficulty("PYTHON", TaskDifficulty.BEGINNER, TaskType.CODING, 5);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(skillService, times(1)).getSkillByName("PYTHON");
    }

    @Test
    void testCheckTaskAvailability_Success() {
        when(skillService.getSkillByName("PYTHON")).thenReturn(testSkill);
        when(taskRepository.countBySkillAndDifficulty(skillId, TaskDifficulty.BEGINNER, true))
                .thenReturn(10L);

        TaskAvailabilityDTO result = taskService.checkTaskAvailability("PYTHON", TaskDifficulty.BEGINNER);

        assertNotNull(result);
        assertEquals("PYTHON", result.skillName());
        assertEquals(TaskDifficulty.BEGINNER, result.difficulty());
        assertEquals(10, result.availableTaskCount());
        assertFalse(result.needsGeneration());
    }

    @Test
    void testCheckTaskAvailability_NeedsGeneration() {
        when(skillService.getSkillByName("PYTHON")).thenReturn(testSkill);
        when(taskRepository.countBySkillAndDifficulty(skillId, TaskDifficulty.BEGINNER, true))
                .thenReturn(2L);

        TaskAvailabilityDTO result = taskService.checkTaskAvailability("PYTHON", TaskDifficulty.BEGINNER);

        assertNotNull(result);
        assertTrue(result.needsGeneration());
        assertEquals(2, result.availableTaskCount());
    }

    @Test
    void testGetTasksForSkillAndDifficulty_CacheMiss_AnonymousUser() {
        List<Task> cachedTasks = new ArrayList<>();

        when(skillService.getSkillByName("PYTHON")).thenReturn(testSkill);
        when(taskRepository.findBySkillIdAndDifficultyAndType(
                skillId, TaskDifficulty.BEGINNER, TaskType.CODING, true, PageRequest.of(0, 5)))
                .thenReturn(cachedTasks);
        when(redisOps.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);

        List<TaskDTO> result = taskService.getTasksForSkillAndDifficulty("PYTHON", TaskDifficulty.BEGINNER, TaskType.CODING, 5);

        assertNotNull(result);
        assertEquals(0, result.size());
        verify(taskEventProducer, never()).requestBatchTaskGeneration(any());
    }

    @Test
    void testGetPersonalizedTasks_CacheMiss_GenerationTriggered() {
        List<Task> cachedTasks = new ArrayList<>();

        when(userSkillProfileRepository.findByIdUserIdAndSkillName(userId, "PYTHON"))
                .thenReturn(Optional.of(userSkillProfile));
        when(taskRepository.findBySkillIdAndDifficultyAndType(
                skillId, TaskDifficulty.BEGINNER, TaskType.CODING, true, PageRequest.of(0, 5)))
                .thenReturn(cachedTasks);
        when(redisOps.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);

        List<TaskDTO> result = taskService.getPersonalizedTasks(userId, "PYTHON", TaskType.CODING, 5);

        assertNotNull(result);
        verify(taskEventProducer, times(1)).requestBatchTaskGeneration(any(BatchGenerationRequest.class));
    }

    @Test
    void testGetPersonalizedTasks_LockAlreadyAcquired() {
        List<Task> cachedTasks = new ArrayList<>();

        when(userSkillProfileRepository.findByIdUserIdAndSkillName(userId, "PYTHON"))
                .thenReturn(Optional.of(userSkillProfile));
        when(taskRepository.findBySkillIdAndDifficultyAndType(
                skillId, TaskDifficulty.BEGINNER, TaskType.CODING, true, PageRequest.of(0, 5)))
                .thenReturn(cachedTasks);
        when(redisOps.setIfAbsent(anyString(), anyString(), any())).thenReturn(false);

        List<TaskDTO> result = taskService.getPersonalizedTasks(userId, "PYTHON", TaskType.CODING, 5);

        assertNotNull(result);
        assertEquals(0, result.size());
        verify(taskEventProducer, never()).requestBatchTaskGeneration(any());
    }

    @Test
    void testRequestSpecificTaskGeneration() {
        com.amalitech.task.service.dto.request.GenerateTaskRequest request =
                new com.amalitech.task.service.dto.request.GenerateTaskRequest(
                        userId,
                        TaskType.CODING,
                        "PYTHON",
                        TaskDifficulty.INTERMEDIATE,
                        "String Manipulation",
                        "Python"
                );

        taskService.requestSpecificTaskGeneration(request, userId);

        verify(taskEventProducer, times(1)).requestSpecificTaskGeneration(any());
    }

    // ==================== ADMIN METHODS TESTS ====================

    @Test
    void testGetAllTasksForAdmin_Success() {
        Page<Task> taskPage = new PageImpl<>(List.of(testTask), PageRequest.of(0, 20), 1);
        AdminTaskSummaryResponse summaryResponse = new AdminTaskSummaryResponse(
                taskId, "Test Task", TaskType.CODING.name(), TaskDifficulty.BEGINNER.name(), 1, false, null
        );

        when(taskRepository.findAll(any(Pageable.class))).thenReturn(taskPage);
        when(taskMapper.toAdminSummaryDTO(testTask)).thenReturn(summaryResponse);

        Page<AdminTaskSummaryResponse> result = taskService.getAllTasksForAdmin(PageRequest.of(0, 20));

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(taskId, result.getContent().get(0).taskId());
        verify(taskRepository, times(1)).findAll(any(Pageable.class));
        verify(taskMapper, times(1)).toAdminSummaryDTO(testTask);
    }

    @Test
    void testGetAllTasksForAdmin_EmptyPage() {
        Page<Task> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        when(taskRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

        Page<AdminTaskSummaryResponse> result = taskService.getAllTasksForAdmin(PageRequest.of(0, 20));

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
        verify(taskRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    void testGetAllTasksForAdmin_MultiplePages() {
        Task task2 = Task.builder()
                .id(UUID.randomUUID())
                .title("Task 2")
                .type(TaskType.ESSAY)
                .difficulty(TaskDifficulty.INTERMEDIATE)
                .build();

        AdminTaskSummaryResponse summary1 = new AdminTaskSummaryResponse(
                testTask.getId(), "Test Task", TaskType.CODING.name(), TaskDifficulty.BEGINNER.name(), 1, false, null
        );
        AdminTaskSummaryResponse summary2 = new AdminTaskSummaryResponse(
                task2.getId(), "Task 2", TaskType.ESSAY.name(), TaskDifficulty.INTERMEDIATE.name(), 1, false, null
        );

        Page<Task> taskPage = new PageImpl<>(
                List.of(testTask, task2),
                PageRequest.of(0, 20),
                50
        );

        when(taskRepository.findAll(any(Pageable.class))).thenReturn(taskPage);
        when(taskMapper.toAdminSummaryDTO(testTask)).thenReturn(summary1);
        when(taskMapper.toAdminSummaryDTO(task2)).thenReturn(summary2);

        Page<AdminTaskSummaryResponse> result = taskService.getAllTasksForAdmin(PageRequest.of(0, 20));

        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(50, result.getTotalElements());
        verify(taskRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    void testGetAllTasksForAdmin_SecondPage() {
        Page<Task> taskPage = new PageImpl<>(List.of(testTask), PageRequest.of(1, 20), 45);

        AdminTaskSummaryResponse response = new AdminTaskSummaryResponse(
                taskId, "Test Task", TaskType.CODING.name(), TaskDifficulty.BEGINNER.name(), 1, false, null
        );
        when(taskRepository.findAll(any(Pageable.class))).thenReturn(taskPage);
        when(taskMapper.toAdminSummaryDTO(testTask)).thenReturn(response);

        Page<AdminTaskSummaryResponse> result = taskService.getAllTasksForAdmin(PageRequest.of(1, 20));

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getNumber());
        verify(taskRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    void testGetTaskForAdmin_Success() {
        AdminTaskDetailResponse detailResponse = new AdminTaskDetailResponse(
                taskId, skillId, "Test Task", "Test Description", 
                "CODING", TaskDifficulty.BEGINNER.name(),
                "PYTHON", null, 1, false, 30, 50, 
                null, null
        );

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(testTask));
        when(taskMapper.toAdminDetailDTO(any(Task.class))).thenReturn(detailResponse);

        AdminTaskDetailResponse result = taskService.getTaskForAdmin(taskId);

        assertNotNull(result);
        assertEquals(taskId, result.taskId());
        assertEquals("Test Task", result.title());
        verify(taskRepository, times(1)).findById(taskId);
        verify(taskMapper, times(1)).toAdminDetailDTO(any(Task.class));
    }

    @Test
    void testGetTaskForAdmin_NotFound() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            taskService.getTaskForAdmin(taskId);
        });

        verify(taskRepository, times(1)).findById(taskId);
    }

    @Test
    void testGetTaskForAdmin_ReturnsDetailedInfo() {
        testTask.setXpReward(100);
        testTask.setEstimatedDurationInMinutes(30);

        AdminTaskDetailResponse detailResponse = new AdminTaskDetailResponse(
                taskId, skillId, "Test Task", "Test Description",
                TaskType.CODING.name(), TaskDifficulty.BEGINNER.name(),
                "PYTHON", null, 1, true, 30, 100,
                null, null
        );

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(testTask));
        when(taskMapper.toAdminDetailDTO(testTask)).thenReturn(detailResponse);

        AdminTaskDetailResponse result = taskService.getTaskForAdmin(taskId);

        assertNotNull(result);
        assertEquals(100, result.xpReward());
        assertEquals(30, result.estimatedDurationInMinutes());
        assertTrue(result.isPublished());
    }

    @Test
    void testGetTaskForAdmin_DifferentTaskTypes() {
        AdminTaskDetailResponse detailResponse = new AdminTaskDetailResponse(
                taskId, skillId, "Test Task", "Test Description",
                "CODING", TaskDifficulty.BEGINNER.name(),
                "PYTHON", null, 1, false, 30, 50,
                null, null
        );

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(testTask));
        when(taskMapper.toAdminDetailDTO(any(Task.class))).thenReturn(detailResponse);

        for (TaskType taskType : TaskType.values()) {
            testTask.setType(taskType);
            AdminTaskDetailResponse result = taskService.getTaskForAdmin(taskId);
            assertNotNull(result);
        }
    }

    // ==================== AVAILABILITY TESTS ====================

    @Test
    void testCheckTaskAvailability_EdgeCase_ExactlyThreshold() {
        when(skillService.getSkillByName("PYTHON")).thenReturn(testSkill);
        when(taskRepository.countBySkillAndDifficulty(skillId, TaskDifficulty.BEGINNER, true))
                .thenReturn(5L);

        TaskAvailabilityDTO result = taskService.checkTaskAvailability("PYTHON", TaskDifficulty.BEGINNER);

        assertFalse(result.needsGeneration());
        assertEquals(5, result.availableTaskCount());
    }

    @Test
    void testCheckTaskAvailability_ZeroTasks() {
        when(skillService.getSkillByName("PYTHON")).thenReturn(testSkill);
        when(taskRepository.countBySkillAndDifficulty(skillId, TaskDifficulty.BEGINNER, true))
                .thenReturn(0L);

        TaskAvailabilityDTO result = taskService.checkTaskAvailability("PYTHON", TaskDifficulty.BEGINNER);

        assertTrue(result.needsGeneration());
        assertEquals(0, result.availableTaskCount());
    }

    @Test
    void testCheckTaskAvailability_AllDifficulties() {
        when(skillService.getSkillByName("PYTHON")).thenReturn(testSkill);

        for (TaskDifficulty difficulty : TaskDifficulty.values()) {
            when(taskRepository.countBySkillAndDifficulty(skillId, difficulty, true))
                    .thenReturn(10L);

            TaskAvailabilityDTO result = taskService.checkTaskAvailability("PYTHON", difficulty);

            assertNotNull(result);
            assertEquals(difficulty, result.difficulty());
        }
    }

    @Test
    void testCheckTaskAvailability_SkillNotFound() {
        when(skillService.getSkillByName("UNKNOWN"))
                .thenThrow(new ResourceNotFoundException("Skill not found"));

        assertThrows(ResourceNotFoundException.class, () -> {
            taskService.checkTaskAvailability("UNKNOWN", TaskDifficulty.BEGINNER);
        });
    }

    // ==================== PAGINATION TESTS ====================

    @Test
    void testGetAllTasksForAdmin_PageSize50() {
        Page<Task> taskPage = new PageImpl<>(List.of(testTask), PageRequest.of(0, 50), 1);
        AdminTaskSummaryResponse summaryResponse = new AdminTaskSummaryResponse(
                taskId, "Test Task", TaskType.CODING.name(), TaskDifficulty.BEGINNER.name(), 1, false, null
        );

        when(taskRepository.findAll(any(Pageable.class))).thenReturn(taskPage);
        when(taskMapper.toAdminSummaryDTO(testTask)).thenReturn(summaryResponse);

        Page<AdminTaskSummaryResponse> result = taskService.getAllTasksForAdmin(PageRequest.of(0, 50));

        assertNotNull(result);
        assertEquals(50, result.getSize());
        verify(taskRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    void testGetAllTasksForAdmin_LargePage() {
        List<Task> tasks = new ArrayList<>();
        List<AdminTaskSummaryResponse> summaries = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            Task task = Task.builder()
                    .id(UUID.randomUUID())
                    .title("Task " + i)
                    .type(TaskType.CODING)
                    .difficulty(TaskDifficulty.BEGINNER)
                    .build();
            tasks.add(task);

            AdminTaskSummaryResponse summary = new AdminTaskSummaryResponse(
                    task.getId(), "Task " + i, TaskType.CODING.name(), TaskDifficulty.BEGINNER.name(), 1, false, null
            );
            summaries.add(summary);
        }

        Page<Task> taskPage = new PageImpl<>(tasks, PageRequest.of(0, 100), 150);

        when(taskRepository.findAll(any(Pageable.class))).thenReturn(taskPage);

        for (int i = 0; i < tasks.size(); i++) {
            when(taskMapper.toAdminSummaryDTO(tasks.get(i))).thenReturn(summaries.get(i));
        }

        Page<AdminTaskSummaryResponse> result = taskService.getAllTasksForAdmin(PageRequest.of(0, 100));

        assertNotNull(result);
        assertEquals(100, result.getContent().size());
    }

    @Test
    void testGetOrGenerateTasks_ExceptionDuringPublishing() {
        List<Task> cachedTasks = new ArrayList<>();
        when(userSkillProfileRepository.findByIdUserIdAndSkillName(userId, "PYTHON"))
                .thenReturn(Optional.of(userSkillProfile));
        when(taskRepository.findBySkillIdAndDifficultyAndType(
                skillId, TaskDifficulty.BEGINNER, TaskType.CODING, true, PageRequest.of(0, 5)))
                .thenReturn(cachedTasks);
        when(redisOps.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);
        doThrow(new RuntimeException("Test Exception")).when(taskEventProducer).requestBatchTaskGeneration(any());

        taskService.getPersonalizedTasks(userId, "PYTHON", TaskType.CODING, 5);

        verify(redisTemplate, times(1)).delete(anyString());
    }

    // ==================== USER TASKS GROUPED BY STATUS TESTS ====================

    @Test
    void testGetUserTasksGroupedByStatus_Success_WithUserSkills() {
        UUID skillId2 = UUID.randomUUID();
        UserSkillProfile userSkillProfile2 = UserSkillProfile.builder()
                .id(new UserSkillProfile.UserSkillId(userId, skillId2))
                .skillName("JAVA")
                .difficulty(TaskDifficulty.INTERMEDIATE)
                .build();

        List<UserSkillProfile> userProfiles = List.of(userSkillProfile, userSkillProfile2);
        Set<UUID> completedTaskIds = Set.of(UUID.randomUUID());

        Task task1 = Task.builder()
                .id(UUID.randomUUID())
                .title("Python Beginner Task")
                .type(TaskType.CODING)
                .difficulty(TaskDifficulty.BEGINNER)
                .build();

        Task task2 = Task.builder()
                .id(UUID.randomUUID())
                .title("Java Intermediate Task")
                .type(TaskType.CODING)
                .difficulty(TaskDifficulty.INTERMEDIATE)
                .build();

        Page<Task> pendingPage = new PageImpl<>(List.of(task1, task2), PageRequest.of(0, 10), 2);
        Page<TaskSubmission> completedSubmissionPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        when(userSkillProfileRepository.findById_UserId(userId)).thenReturn(userProfiles);
        when(submissionRepository.findCompletedTaskIdsByUser(userId)).thenReturn(completedTaskIds);
        when(taskRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pendingPage);
        when(submissionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(completedSubmissionPage);
        when(taskMapper.toDTO(any(Task.class))).thenReturn(testTaskDTO);

        com.amalitech.task.service.dto.response.UserTasksResponse result = 
            taskService.getUserTasksGroupedByStatus(userId, 0, 10, 0, 10, null, "ALL_PERIODS");

        assertNotNull(result);
        assertNotNull(result.pending());
        assertNotNull(result.completed());
        assertEquals(2, result.pending().getTotalElements());
        assertEquals(0, result.completed().getTotalElements());
        verify(userSkillProfileRepository, times(1)).findById_UserId(userId);
        verify(submissionRepository, times(1)).findCompletedTaskIdsByUser(userId);
    }

    @Test
    void testGetUserTasksGroupedByStatus_NoUserSkillProfiles_EmptyResponse() {
        when(userSkillProfileRepository.findById_UserId(userId)).thenReturn(List.of());

        com.amalitech.task.service.dto.response.UserTasksResponse result = 
            taskService.getUserTasksGroupedByStatus(userId, 0, 10, 0, 10, null, "ALL_PERIODS");

        assertNotNull(result);
        assertEquals(0, result.pending().getTotalElements());
        assertEquals(0, result.completed().getTotalElements());
        verify(userSkillProfileRepository, times(1)).findById_UserId(userId);
        verify(taskRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void testGetUserTasksGroupedByStatus_SingleSkillProfile() {
        List<UserSkillProfile> userProfiles = List.of(userSkillProfile);
        Set<UUID> completedTaskIds = Set.of();

        List<Task> pendingTasks = List.of(testTask, testTask, testTask, testTask, testTask);
        Page<Task> pendingPage = new PageImpl<>(pendingTasks, PageRequest.of(0, 10), 5);
        Page<TaskSubmission> completedSubmissionPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        when(userSkillProfileRepository.findById_UserId(userId)).thenReturn(userProfiles);
        when(submissionRepository.findCompletedTaskIdsByUser(userId)).thenReturn(completedTaskIds);
        when(taskRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pendingPage);
        when(submissionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(completedSubmissionPage);
        when(taskMapper.toDTO(testTask)).thenReturn(testTaskDTO);

        com.amalitech.task.service.dto.response.UserTasksResponse result = 
            taskService.getUserTasksGroupedByStatus(userId, 0, 10, 0, 10, null, "ALL_PERIODS");

        assertNotNull(result);
        assertEquals(5, result.pending().getTotalElements());
        assertEquals(0, result.completed().getTotalElements());
        verify(taskRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void testGetUserTasksGroupedByStatus_MultipleSkillProfiles() {
        UUID skillId2 = UUID.randomUUID();
        UUID skillId3 = UUID.randomUUID();

        UserSkillProfile profile2 = UserSkillProfile.builder()
                .id(new UserSkillProfile.UserSkillId(userId, skillId2))
                .skillName("JAVA")
                .difficulty(TaskDifficulty.INTERMEDIATE)
                .build();

        UserSkillProfile profile3 = UserSkillProfile.builder()
                .id(new UserSkillProfile.UserSkillId(userId, skillId3))
                .skillName("CPP")
                .difficulty(TaskDifficulty.ADVANCED)
                .build();

        List<UserSkillProfile> userProfiles = List.of(userSkillProfile, profile2, profile3);
        Set<UUID> completedTaskIds = Set.of();

        Page<Task> pendingPage = new PageImpl<>(List.of(testTask), PageRequest.of(0, 10), 20);
        Page<TaskSubmission> completedSubmissionPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        when(userSkillProfileRepository.findById_UserId(userId)).thenReturn(userProfiles);
        when(submissionRepository.findCompletedTaskIdsByUser(userId)).thenReturn(completedTaskIds);
        when(taskRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pendingPage);
        when(submissionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(completedSubmissionPage);
        when(taskMapper.toDTO(testTask)).thenReturn(testTaskDTO);

        com.amalitech.task.service.dto.response.UserTasksResponse result = 
            taskService.getUserTasksGroupedByStatus(userId, 0, 10, 0, 10, null, "ALL_PERIODS");

        assertNotNull(result);
        assertEquals(20, result.pending().getTotalElements());
        verify(userSkillProfileRepository, times(1)).findById_UserId(userId);
    }

    @Test
    void testGetUserTasksGroupedByStatus_NoCompletedTasks() {
        List<UserSkillProfile> userProfiles = List.of(userSkillProfile);
        Set<UUID> completedTaskIds = Set.of();

        Page<Task> pendingPage = new PageImpl<>(List.of(testTask), PageRequest.of(0, 10), 10);
        Page<TaskSubmission> completedSubmissionPage = Page.empty(PageRequest.of(0, 10));

        when(userSkillProfileRepository.findById_UserId(userId)).thenReturn(userProfiles);
        when(submissionRepository.findCompletedTaskIdsByUser(userId)).thenReturn(completedTaskIds);
        when(taskRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pendingPage);
        when(submissionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(completedSubmissionPage);
        when(taskMapper.toDTO(testTask)).thenReturn(testTaskDTO);

        com.amalitech.task.service.dto.response.UserTasksResponse result = 
            taskService.getUserTasksGroupedByStatus(userId, 0, 10, 0, 10, null, "ALL_PERIODS");

        assertNotNull(result);
        assertTrue(result.pending().getTotalElements() > 0);
        assertEquals(0, result.completed().getTotalElements());
        verify(taskRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
        verify(submissionRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void testGetUserTasksGroupedByStatus_SomeCompletedTasks() {
        List<UserSkillProfile> userProfiles = List.of(userSkillProfile);
        UUID completedTaskId1 = UUID.randomUUID();
        UUID completedTaskId2 = UUID.randomUUID();
        Set<UUID> completedTaskIds = Set.of(completedTaskId1, completedTaskId2);

        Task completedTask1 = Task.builder()
                .id(completedTaskId1)
                .title("Completed Task 1")
                .type(TaskType.CODING)
                .difficulty(TaskDifficulty.BEGINNER)
                .build();

        Task completedTask2 = Task.builder()
                .id(completedTaskId2)
                .title("Completed Task 2")
                .type(TaskType.ESSAY)
                .difficulty(TaskDifficulty.BEGINNER)
                .build();

        Page<Task> pendingPage = new PageImpl<>(List.of(testTask), PageRequest.of(0, 10), 15);
        Page<TaskSubmission> completedSubmissionPage = new PageImpl<>(List.of(createTaskSubmission(completedTask1), createTaskSubmission(completedTask2)), PageRequest.of(0, 10), 2);

        when(userSkillProfileRepository.findById_UserId(userId)).thenReturn(userProfiles);
        when(submissionRepository.findCompletedTaskIdsByUser(userId)).thenReturn(completedTaskIds);
        when(taskRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pendingPage);
        when(submissionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(completedSubmissionPage);
        when(taskMapper.toDTO(any(Task.class))).thenReturn(testTaskDTO);

        com.amalitech.task.service.dto.response.UserTasksResponse result = 
            taskService.getUserTasksGroupedByStatus(userId, 0, 10, 0, 10, null, "ALL_PERIODS");

        assertNotNull(result);
        assertEquals(15, result.pending().getTotalElements());
        assertEquals(2, result.completed().getTotalElements());
        verify(submissionRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void testGetUserTasksGroupedByStatus_AllTasksCompleted() {
        List<UserSkillProfile> userProfiles = List.of(userSkillProfile);
        UUID completedTaskId = UUID.randomUUID();
        Set<UUID> completedTaskIds = Set.of(completedTaskId);

        Task completedTask = Task.builder()
                .id(completedTaskId)
                .title("Completed Task")
                .type(TaskType.CODING)
                .difficulty(TaskDifficulty.BEGINNER)
                .build();

        Page<Task> pendingPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        
        List<TaskSubmission> completedSubmissions = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            completedSubmissions.add(createTaskSubmission(completedTask));
        }
        Page<TaskSubmission> completedSubmissionPage = new PageImpl<>(completedSubmissions, PageRequest.of(0, 10), 8);

        when(userSkillProfileRepository.findById_UserId(userId)).thenReturn(userProfiles);
        when(submissionRepository.findCompletedTaskIdsByUser(userId)).thenReturn(completedTaskIds);
        when(taskRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pendingPage);
        when(submissionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(completedSubmissionPage);
        when(taskMapper.toDTO(completedTask)).thenReturn(testTaskDTO);

        com.amalitech.task.service.dto.response.UserTasksResponse result = 
            taskService.getUserTasksGroupedByStatus(userId, 0, 10, 0, 10, null, "ALL_PERIODS");

        assertNotNull(result);
        assertEquals(0, result.pending().getTotalElements());
        assertEquals(8, result.completed().getTotalElements());
    }

    @Test
    void testGetUserTasksGroupedByStatus_PendingPage0Size10() {
        List<UserSkillProfile> userProfiles = List.of(userSkillProfile);
        Set<UUID> completedTaskIds = Set.of();

        List<Task> tasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tasks.add(Task.builder()
                    .id(UUID.randomUUID())
                    .title("Task " + i)
                    .type(TaskType.CODING)
                    .difficulty(TaskDifficulty.BEGINNER)
                    .build());
        }

        Page<Task> pendingPage = new PageImpl<>(tasks, PageRequest.of(0, 10), 50);
        Page<TaskSubmission> completedSubmissionPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        when(userSkillProfileRepository.findById_UserId(userId)).thenReturn(userProfiles);
        when(submissionRepository.findCompletedTaskIdsByUser(userId)).thenReturn(completedTaskIds);
        when(taskRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pendingPage);
        when(submissionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(completedSubmissionPage);
        when(taskMapper.toDTO(any(Task.class))).thenReturn(testTaskDTO);

        com.amalitech.task.service.dto.response.UserTasksResponse result = 
            taskService.getUserTasksGroupedByStatus(userId, 0, 10, 0, 10, null, "ALL_PERIODS");

        assertNotNull(result);
        assertEquals(10, result.pending().getContent().size());
        assertEquals(50, result.pending().getTotalElements());
        assertEquals(0, result.pending().getNumber());
    }

    @Test
    void testGetUserTasksGroupedByStatus_PendingPage1Size10() {
        List<UserSkillProfile> userProfiles = List.of(userSkillProfile);
        Set<UUID> completedTaskIds = Set.of();

        List<Task> tasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tasks.add(Task.builder()
                    .id(UUID.randomUUID())
                    .title("Task " + i)
                    .type(TaskType.CODING)
                    .difficulty(TaskDifficulty.BEGINNER)
                    .build());
        }

        Page<Task> pendingPage = new PageImpl<>(tasks, PageRequest.of(1, 10), 50);
        Page<TaskSubmission> completedSubmissionPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        when(userSkillProfileRepository.findById_UserId(userId)).thenReturn(userProfiles);
        when(submissionRepository.findCompletedTaskIdsByUser(userId)).thenReturn(completedTaskIds);
        when(taskRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pendingPage);
        when(submissionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(completedSubmissionPage);
        when(taskMapper.toDTO(any(Task.class))).thenReturn(testTaskDTO);

        com.amalitech.task.service.dto.response.UserTasksResponse result = 
            taskService.getUserTasksGroupedByStatus(userId, 1, 10, 0, 10, null, "ALL_PERIODS");

        assertNotNull(result);
        assertEquals(1, result.pending().getNumber());
        assertEquals(10, result.pending().getSize());
        assertEquals(50, result.pending().getTotalElements());
    }

    @Test
    void testGetUserTasksGroupedByStatus_PendingCustomPageSize() {
        List<UserSkillProfile> userProfiles = List.of(userSkillProfile);
        Set<UUID> completedTaskIds = Set.of();

        List<Task> tasks = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            tasks.add(Task.builder()
                    .id(UUID.randomUUID())
                    .title("Task " + i)
                    .type(TaskType.CODING)
                    .difficulty(TaskDifficulty.BEGINNER)
                    .build());
        }

        Page<Task> pendingPage = new PageImpl<>(tasks, PageRequest.of(0, 20), 60);
        Page<TaskSubmission> completedSubmissionPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        when(userSkillProfileRepository.findById_UserId(userId)).thenReturn(userProfiles);
        when(submissionRepository.findCompletedTaskIdsByUser(userId)).thenReturn(completedTaskIds);
        when(taskRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pendingPage);
        when(submissionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(completedSubmissionPage);
        when(taskMapper.toDTO(any(Task.class))).thenReturn(testTaskDTO);

        com.amalitech.task.service.dto.response.UserTasksResponse result = 
            taskService.getUserTasksGroupedByStatus(userId, 0, 20, 0, 10, null, "ALL_PERIODS");

        assertNotNull(result);
        assertEquals(20, result.pending().getSize());
        assertEquals(20, result.pending().getContent().size());
    }

    @Test
    void testGetUserTasksGroupedByStatus_CompletedPage0Size10() {
        List<UserSkillProfile> userProfiles = List.of(userSkillProfile);
        UUID completedTaskId = UUID.randomUUID();
        Set<UUID> completedTaskIds = Set.of(completedTaskId);

        Page<Task> pendingPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        List<TaskSubmission> completedSubmissions = new ArrayList<>();
        List<Task> tasksList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Task task = Task.builder()
                    .id(UUID.randomUUID())
                    .title("Completed " + i)
                    .type(TaskType.CODING)
                    .difficulty(TaskDifficulty.BEGINNER)
                    .build();
            tasksList.add(task);
            completedSubmissions.add(createTaskSubmission(task));
        }

        Page<TaskSubmission> completedSubmissionPage = new PageImpl<>(completedSubmissions, PageRequest.of(0, 10), 30);

        when(userSkillProfileRepository.findById_UserId(userId)).thenReturn(userProfiles);
        when(submissionRepository.findCompletedTaskIdsByUser(userId)).thenReturn(completedTaskIds);
        when(taskRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pendingPage);
        when(submissionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(completedSubmissionPage);
        when(taskMapper.toDTO(any(Task.class))).thenReturn(testTaskDTO);

        com.amalitech.task.service.dto.response.UserTasksResponse result = 
            taskService.getUserTasksGroupedByStatus(userId, 0, 10, 0, 10, null, "ALL_PERIODS");

        assertNotNull(result);
        assertEquals(10, result.completed().getSize());
        assertEquals(0, result.completed().getNumber());
        assertEquals(30, result.completed().getTotalElements());
    }

    @Test
    void testGetUserTasksGroupedByStatus_CompletedPage1Size10() {
        List<UserSkillProfile> userProfiles = List.of(userSkillProfile);
        UUID completedTaskId = UUID.randomUUID();
        Set<UUID> completedTaskIds = Set.of(completedTaskId);

        Page<Task> pendingPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        List<TaskSubmission> completedSubmissions = new ArrayList<>();
        List<Task> tasksList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Task task = Task.builder()
                    .id(UUID.randomUUID())
                    .title("Completed " + i)
                    .type(TaskType.CODING)
                    .difficulty(TaskDifficulty.BEGINNER)
                    .build();
            tasksList.add(task);
            completedSubmissions.add(createTaskSubmission(task));
        }

        Page<TaskSubmission> completedSubmissionPage = new PageImpl<>(completedSubmissions, PageRequest.of(1, 10), 30);

        when(userSkillProfileRepository.findById_UserId(userId)).thenReturn(userProfiles);
        when(submissionRepository.findCompletedTaskIdsByUser(userId)).thenReturn(completedTaskIds);
        when(taskRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pendingPage);
        when(submissionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(completedSubmissionPage);
        when(taskMapper.toDTO(any(Task.class))).thenReturn(testTaskDTO);

        com.amalitech.task.service.dto.response.UserTasksResponse result = 
            taskService.getUserTasksGroupedByStatus(userId, 0, 10, 1, 10, null, "ALL_PERIODS");

        assertNotNull(result);
        assertEquals(1, result.completed().getNumber());
        assertEquals(30, result.completed().getTotalElements());
    }

    @Test
    void testGetUserTasksGroupedByStatus_IndependentPagination_DifferentSizes() {
        List<UserSkillProfile> userProfiles = List.of(userSkillProfile);
        UUID completedTaskId = UUID.randomUUID();
        Set<UUID> completedTaskIds = Set.of(completedTaskId);

        List<Task> pendingTasks = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            pendingTasks.add(Task.builder()
                    .id(UUID.randomUUID())
                    .title("Pending " + i)
                    .type(TaskType.CODING)
                    .difficulty(TaskDifficulty.BEGINNER)
                    .build());
        }

        List<TaskSubmission> completedSubmissions = new ArrayList<>();
        List<Task> completedTasks = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Task task = Task.builder()
                    .id(UUID.randomUUID())
                    .title("Completed " + i)
                    .type(TaskType.CODING)
                    .difficulty(TaskDifficulty.BEGINNER)
                    .build();
            completedTasks.add(task);
            completedSubmissions.add(createTaskSubmission(task));
        }

        Page<Task> pendingPage = new PageImpl<>(pendingTasks, PageRequest.of(0, 5), 25);
        Page<TaskSubmission> completedSubmissionPage = new PageImpl<>(completedSubmissions, PageRequest.of(0, 20), 60);

        when(userSkillProfileRepository.findById_UserId(userId)).thenReturn(userProfiles);
        when(submissionRepository.findCompletedTaskIdsByUser(userId)).thenReturn(completedTaskIds);
        when(taskRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pendingPage);
        when(submissionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(completedSubmissionPage);
        when(taskMapper.toDTO(any(Task.class))).thenReturn(testTaskDTO);

        com.amalitech.task.service.dto.response.UserTasksResponse result = 
            taskService.getUserTasksGroupedByStatus(userId, 0, 5, 0, 20, null, "ALL_PERIODS");

        assertNotNull(result);
        assertEquals(5, result.pending().getSize());
        assertEquals(5, result.pending().getContent().size());
        assertEquals(20, result.completed().getSize());
        assertEquals(20, result.completed().getContent().size());
    }

    @Test
    void testGetUserTasksGroupedByStatus_SkillAndDifficultyMatching() {
        List<UserSkillProfile> userProfiles = List.of(userSkillProfile);
        Set<UUID> completedTaskIds = Set.of();

        Page<Task> pendingPage = new PageImpl<>(List.of(testTask), PageRequest.of(0, 10), 1);
        Page<TaskSubmission> completedSubmissionPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        when(userSkillProfileRepository.findById_UserId(userId)).thenReturn(userProfiles);
        when(submissionRepository.findCompletedTaskIdsByUser(userId)).thenReturn(completedTaskIds);
        when(taskRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pendingPage);
        when(submissionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(completedSubmissionPage);
        when(taskMapper.toDTO(testTask)).thenReturn(testTaskDTO);

        com.amalitech.task.service.dto.response.UserTasksResponse result = 
            taskService.getUserTasksGroupedByStatus(userId, 0, 10, 0, 10, null, "ALL_PERIODS");

        assertNotNull(result);
        assertEquals(1, result.pending().getTotalElements());
        verify(taskRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void testGetUserTasksGroupedByStatus_TaskDTOMapping() {
        List<UserSkillProfile> userProfiles = List.of(userSkillProfile);
        Set<UUID> completedTaskIds = Set.of();

        Task task1 = Task.builder()
                .id(UUID.randomUUID())
                .title("Task 1")
                .type(TaskType.CODING)
                .difficulty(TaskDifficulty.BEGINNER)
                .build();

        Task task2 = Task.builder()
                .id(UUID.randomUUID())
                .title("Task 2")
                .type(TaskType.ESSAY)
                .difficulty(TaskDifficulty.INTERMEDIATE)
                .build();

        Page<Task> pendingPage = new PageImpl<>(List.of(task1, task2), PageRequest.of(0, 10), 2);
        Page<TaskSubmission> completedSubmissionPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        when(userSkillProfileRepository.findById_UserId(userId)).thenReturn(userProfiles);
        when(submissionRepository.findCompletedTaskIdsByUser(userId)).thenReturn(completedTaskIds);
        when(taskRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pendingPage);
        when(submissionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(completedSubmissionPage);
        when(taskMapper.toDTO(any(Task.class))).thenReturn(testTaskDTO);

        com.amalitech.task.service.dto.response.UserTasksResponse result = 
            taskService.getUserTasksGroupedByStatus(userId, 0, 10, 0, 10, null, "ALL_PERIODS");

        assertNotNull(result);
        verify(taskMapper, times(2)).toDTO(any(Task.class));
        assertEquals(2, result.pending().getContent().size());
    }

    @Test
    void testGetUserTasksGroupedByStatus_UserWithZeroTasks() {
        List<UserSkillProfile> userProfiles = List.of(userSkillProfile);
        Set<UUID> completedTaskIds = Set.of();

        Page<Task> pendingPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        Page<TaskSubmission> completedSubmissionPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        when(userSkillProfileRepository.findById_UserId(userId)).thenReturn(userProfiles);
        when(submissionRepository.findCompletedTaskIdsByUser(userId)).thenReturn(completedTaskIds);
        when(taskRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pendingPage);
        when(submissionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(completedSubmissionPage);

        com.amalitech.task.service.dto.response.UserTasksResponse result = 
            taskService.getUserTasksGroupedByStatus(userId, 0, 10, 0, 10, null, "ALL_PERIODS");

        assertNotNull(result);
        assertEquals(0, result.pending().getTotalElements());
        assertEquals(0, result.completed().getTotalElements());
    }

    @Test
    void testGetUserTasksGroupedByStatus_LargeDataset() {
        List<UserSkillProfile> userProfiles = List.of(userSkillProfile);
        Set<UUID> completedTaskIds = Set.of();

        List<Task> largePendingList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            largePendingList.add(Task.builder()
                    .id(UUID.randomUUID())
                    .title("Task " + i)
                    .type(TaskType.CODING)
                    .difficulty(TaskDifficulty.BEGINNER)
                    .build());
        }

        Page<Task> pendingPage = new PageImpl<>(largePendingList, PageRequest.of(0, 100), 1000);
        Page<TaskSubmission> completedSubmissionPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        when(userSkillProfileRepository.findById_UserId(userId)).thenReturn(userProfiles);
        when(submissionRepository.findCompletedTaskIdsByUser(userId)).thenReturn(completedTaskIds);
        when(taskRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pendingPage);
        when(submissionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(completedSubmissionPage);
        when(taskMapper.toDTO(any(Task.class))).thenReturn(testTaskDTO);

        com.amalitech.task.service.dto.response.UserTasksResponse result = 
            taskService.getUserTasksGroupedByStatus(userId, 0, 100, 0, 10, null, "ALL_PERIODS");

        assertNotNull(result);
        assertEquals(100, result.pending().getContent().size());
        assertEquals(1000, result.pending().getTotalElements());
    }

    @Test
    void testGetUserTasksGroupedByStatus_AllTasksForDifferentUsers_NoMixup() {
        UUID differentUserId = UUID.randomUUID();
        
        List<UserSkillProfile> userProfiles = List.of(userSkillProfile);
        Set<UUID> completedTaskIds = Set.of();

        Page<Task> pendingPage = new PageImpl<>(List.of(testTask), PageRequest.of(0, 10), 5);
        Page<TaskSubmission> completedSubmissionPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        when(userSkillProfileRepository.findById_UserId(userId)).thenReturn(userProfiles);
        when(submissionRepository.findCompletedTaskIdsByUser(userId)).thenReturn(completedTaskIds);
        when(taskRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pendingPage);
        when(submissionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(completedSubmissionPage);
        when(taskMapper.toDTO(testTask)).thenReturn(testTaskDTO);

        com.amalitech.task.service.dto.response.UserTasksResponse result = 
            taskService.getUserTasksGroupedByStatus(userId, 0, 10, 0, 10, null, "ALL_PERIODS");

        assertNotNull(result);
        verify(userSkillProfileRepository, times(1)).findById_UserId(userId);
        verify(submissionRepository, times(1)).findCompletedTaskIdsByUser(userId);
    }

    @Test
    void testGetUserTasksGroupedByStatus_EmptyCompletedTaskIds() {
        List<UserSkillProfile> userProfiles = List.of(userSkillProfile);
        Set<UUID> completedTaskIds = Set.of();

        Page<Task> pendingPage = new PageImpl<>(List.of(testTask), PageRequest.of(0, 10), 10);
        Page<TaskSubmission> completedSubmissionPage = Page.empty(PageRequest.of(0, 10));

        when(userSkillProfileRepository.findById_UserId(userId)).thenReturn(userProfiles);
        when(submissionRepository.findCompletedTaskIdsByUser(userId)).thenReturn(completedTaskIds);
        when(taskRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pendingPage);
        when(submissionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(completedSubmissionPage);
        when(taskMapper.toDTO(testTask)).thenReturn(testTaskDTO);

        com.amalitech.task.service.dto.response.UserTasksResponse result = 
            taskService.getUserTasksGroupedByStatus(userId, 0, 10, 0, 10, null, "ALL_PERIODS");

        assertNotNull(result);
        assertEquals(10, result.pending().getTotalElements());
        assertEquals(0, result.completed().getTotalElements());
        verify(submissionRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void testGetUserTasksGroupedByStatus_NoUserSkillProfiles() {
        List<UserSkillProfile> emptyProfiles = List.of();
        Set<UUID> completedTaskIds = Set.of();

        Page<Task> emptyPendingPage = Page.empty(PageRequest.of(0, 10));
        Page<TaskSubmission> emptyCompletedPage = Page.empty(PageRequest.of(0, 10));

        when(userSkillProfileRepository.findById_UserId(userId)).thenReturn(emptyProfiles);
        when(submissionRepository.findCompletedTaskIdsByUser(userId)).thenReturn(completedTaskIds);
        when(taskRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPendingPage);
        when(submissionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyCompletedPage);

        com.amalitech.task.service.dto.response.UserTasksResponse result = 
            taskService.getUserTasksGroupedByStatus(userId, 0, 10, 0, 10, null, "ALL_PERIODS");

        assertNotNull(result);
        assertEquals(0, result.pending().getTotalElements());
        assertEquals(0, result.completed().getTotalElements());
    }

    @Test
    void testGetUserTasksGroupedByStatus_OrLogicForMultipleSkills() {
        UUID skillId2 = UUID.randomUUID();
        UUID skillId3 = UUID.randomUUID();

        UserSkillProfile profile2 = UserSkillProfile.builder()
                .id(new UserSkillProfile.UserSkillId(userId, skillId2))
                .skillName("JAVA")
                .difficulty(TaskDifficulty.INTERMEDIATE)
                .build();

        UserSkillProfile profile3 = UserSkillProfile.builder()
                .id(new UserSkillProfile.UserSkillId(userId, skillId3))
                .skillName("CPP")
                .difficulty(TaskDifficulty.ADVANCED)
                .build();

        List<UserSkillProfile> userProfiles = List.of(userSkillProfile, profile2, profile3);
        Set<UUID> completedTaskIds = Set.of();

        List<Task> mixedTasks = List.of(
            Task.builder().id(UUID.randomUUID()).title("Python BEGINNER").difficulty(TaskDifficulty.BEGINNER).build(),
            Task.builder().id(UUID.randomUUID()).title("Java INTERMEDIATE").difficulty(TaskDifficulty.INTERMEDIATE).build(),
            Task.builder().id(UUID.randomUUID()).title("CPP ADVANCED").difficulty(TaskDifficulty.ADVANCED).build()
        );

        Page<Task> pendingPage = new PageImpl<>(mixedTasks, PageRequest.of(0, 10), 3);
        Page<TaskSubmission> completedSubmissionPage = Page.empty(PageRequest.of(0, 10));

        when(userSkillProfileRepository.findById_UserId(userId)).thenReturn(userProfiles);
        when(submissionRepository.findCompletedTaskIdsByUser(userId)).thenReturn(completedTaskIds);
        when(taskRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pendingPage);
        when(submissionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(completedSubmissionPage);
        when(taskMapper.toDTO(any(Task.class))).thenReturn(testTaskDTO);

        com.amalitech.task.service.dto.response.UserTasksResponse result = 
            taskService.getUserTasksGroupedByStatus(userId, 0, 10, 0, 10, null, "ALL_PERIODS");

        assertNotNull(result);
        assertEquals(3, result.pending().getTotalElements());
        verify(taskRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void testGetUserTasksGroupedByStatus_CompletedCustomPageSize() {
        List<UserSkillProfile> userProfiles = List.of(userSkillProfile);
        UUID completedTaskId = UUID.randomUUID();
        Set<UUID> completedTaskIds = Set.of(completedTaskId);

        Page<Task> pendingPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        List<TaskSubmission> completedSubmissions = new ArrayList<>();
        List<Task> completedTasks = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            Task task = Task.builder()
                    .id(UUID.randomUUID())
                    .title("Completed " + i)
                    .type(TaskType.CODING)
                    .difficulty(TaskDifficulty.BEGINNER)
                    .build();
            completedTasks.add(task);
            completedSubmissions.add(createTaskSubmission(task));
        }

        Page<TaskSubmission> completedSubmissionPage = new PageImpl<>(completedSubmissions, PageRequest.of(0, 15), 45);

        when(userSkillProfileRepository.findById_UserId(userId)).thenReturn(userProfiles);
        when(submissionRepository.findCompletedTaskIdsByUser(userId)).thenReturn(completedTaskIds);
        when(taskRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pendingPage);
        when(submissionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(completedSubmissionPage);
        when(taskMapper.toDTO(any(Task.class))).thenReturn(testTaskDTO);

        com.amalitech.task.service.dto.response.UserTasksResponse result = 
            taskService.getUserTasksGroupedByStatus(userId, 0, 10, 0, 15, null, "ALL_PERIODS");

        assertNotNull(result);
        assertEquals(15, result.completed().getSize());
        assertEquals(15, result.completed().getContent().size());
    }

    @Test
    void testGetUserTasksGroupedByStatus_PendingPageBeyondTotal() {
        List<UserSkillProfile> userProfiles = List.of(userSkillProfile);
        Set<UUID> completedTaskIds = Set.of();

        Page<Task> emptyPage = new PageImpl<>(List.of(), PageRequest.of(10, 10), 5);
        Page<TaskSubmission> completedSubmissionPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        when(userSkillProfileRepository.findById_UserId(userId)).thenReturn(userProfiles);
        when(submissionRepository.findCompletedTaskIdsByUser(userId)).thenReturn(completedTaskIds);
        when(taskRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);
        when(submissionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(completedSubmissionPage);

        com.amalitech.task.service.dto.response.UserTasksResponse result = 
            taskService.getUserTasksGroupedByStatus(userId, 10, 10, 0, 10, null, "ALL_PERIODS");

        assertNotNull(result);
        assertEquals(0, result.pending().getContent().size());
        assertEquals(5, result.pending().getTotalElements());
    }

    @Test
    void testGetUserTasksGroupedByStatus_IndependentPagination_DifferentPages() {
        List<UserSkillProfile> userProfiles = List.of(userSkillProfile);
        Set<UUID> completedTaskIds = Set.of();

        List<Task> pendingTasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            pendingTasks.add(Task.builder()
                    .id(UUID.randomUUID())
                    .title("Pending " + i)
                    .type(TaskType.CODING)
                    .difficulty(TaskDifficulty.BEGINNER)
                    .build());
        }

        List<TaskSubmission> completedSubmissions = new ArrayList<>();
        List<Task> completedTasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Task task = Task.builder()
                    .id(UUID.randomUUID())
                    .title("Completed " + i)
                    .type(TaskType.CODING)
                    .difficulty(TaskDifficulty.BEGINNER)
                    .build();
            completedTasks.add(task);
            completedSubmissions.add(createTaskSubmission(task));
        }

        Page<Task> pendingPage = new PageImpl<>(pendingTasks, PageRequest.of(0, 10), 50);
        Page<TaskSubmission> completedSubmissionPage = new PageImpl<>(completedSubmissions, PageRequest.of(2, 10), 50);

        when(userSkillProfileRepository.findById_UserId(userId)).thenReturn(userProfiles);
        when(submissionRepository.findCompletedTaskIdsByUser(userId)).thenReturn(completedTaskIds);
        when(taskRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pendingPage);
        when(submissionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(completedSubmissionPage);
        when(taskMapper.toDTO(any(Task.class))).thenReturn(testTaskDTO);

        com.amalitech.task.service.dto.response.UserTasksResponse result = 
            taskService.getUserTasksGroupedByStatus(userId, 0, 10, 2, 10, null, "ALL_PERIODS");

        assertNotNull(result);
        assertEquals(0, result.pending().getNumber());
        assertEquals(2, result.completed().getNumber());
    }

    @Test
    void testGetUserTasksGroupedByStatus_Success_MultipleDifficulties() {
        UUID skillId2 = UUID.randomUUID();

        UserSkillProfile profile2 = UserSkillProfile.builder()
                .id(new UserSkillProfile.UserSkillId(userId, skillId2))
                .skillName("PYTHON")
                .difficulty(TaskDifficulty.INTERMEDIATE)
                .build();

        List<UserSkillProfile> userProfiles = List.of(userSkillProfile, profile2);
        Set<UUID> completedTaskIds = Set.of();

        Task task1 = Task.builder()
                .id(UUID.randomUUID())
                .title("Python BEGINNER")
                .type(TaskType.CODING)
                .difficulty(TaskDifficulty.BEGINNER)
                .build();

        Task task2 = Task.builder()
                .id(UUID.randomUUID())
                .title("Python INTERMEDIATE")
                .type(TaskType.CODING)
                .difficulty(TaskDifficulty.INTERMEDIATE)
                .build();

        Page<Task> pendingPage = new PageImpl<>(List.of(task1, task2), PageRequest.of(0, 10), 2);
        Page<TaskSubmission> completedSubmissionPage = Page.empty(PageRequest.of(0, 10));

        when(userSkillProfileRepository.findById_UserId(userId)).thenReturn(userProfiles);
        when(submissionRepository.findCompletedTaskIdsByUser(userId)).thenReturn(completedTaskIds);
        when(taskRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pendingPage);
        when(submissionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(completedSubmissionPage);
        when(taskMapper.toDTO(any(Task.class))).thenReturn(testTaskDTO);

        com.amalitech.task.service.dto.response.UserTasksResponse result = 
            taskService.getUserTasksGroupedByStatus(userId, 0, 10, 0, 10, null, "ALL_PERIODS");

        assertNotNull(result);
        assertEquals(2, result.pending().getTotalElements());
    }
}
