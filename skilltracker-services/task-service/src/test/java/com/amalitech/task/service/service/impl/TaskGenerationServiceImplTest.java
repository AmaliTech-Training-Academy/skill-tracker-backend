package com.amalitech.task.service.service.impl;

import com.amalitech.common.event.events.TaskGenerationFailedEvent;
import com.amalitech.common.event.events.TaskGenerationSucceededEvent;
import com.amalitech.common.event.events.UserOnboardingCompletedEvent;
import com.amalitech.task.service.dto.request.BatchGenerationRequest;
import com.amalitech.task.service.dto.request.GenerateTaskRequest;
import com.amalitech.task.service.events.TaskReplyEventProducer;
import com.amalitech.task.service.model.UserSkillProfile;
import com.amalitech.task.service.model.enums.TaskDifficulty;
import com.amalitech.task.service.model.enums.TaskType;
import com.amalitech.task.service.model.view.SkillView;
import com.amalitech.task.service.repository.SkillViewRepository;
import com.amalitech.task.service.repository.TaskRepository;
import com.amalitech.task.service.repository.UserSkillProfileRepository;
import com.amalitech.task.service.service.ContentGeneratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.io.IOException;
import java.time.Duration;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskGenerationServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private SkillViewRepository skillViewRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ContentGeneratorService contentGeneratorService;

    @Mock
    private TaskReplyEventProducer replyEventProducer;

    @Mock
    private UserSkillProfileRepository userSkillProfileRepository;

    @Mock
    private ValueOperations<String, String> redisOps;

    private TaskGenerationServiceImpl taskGenerationService;

    private UUID userId;
    private UUID skillId;
    private SkillView testSkill;
    private BatchGenerationRequest batchRequest;
    private GenerateTaskRequest adminRequest;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        skillId = UUID.randomUUID();

        testSkill = new SkillView();
        testSkill.setId(skillId);
        testSkill.setName("PYTHON");

        batchRequest = new BatchGenerationRequest(
                userId,
                "PYTHON",
                TaskDifficulty.INTERMEDIATE,
                5,
                TaskType.CODING
        );

        adminRequest = new GenerateTaskRequest(
                userId,
                TaskType.ESSAY,
                "PYTHON",
                TaskDifficulty.ADVANCED,
                "Design Patterns",
                "Python"
        );

        // Manually instantiate service with mocks (constructor requires @Value parameters)
        taskGenerationService = new TaskGenerationServiceImpl(
                redisTemplate,
                skillViewRepository,
                taskRepository,
                contentGeneratorService,
                replyEventProducer,
                userSkillProfileRepository,
                5,   // codingOnboardingQuantity
                5,   // mcqOnboardingTaskQuantity
                10,  // mcqOnboardingQuestionsPerTask
                5    // essayOnboardingQuantity
        );
    }

    // ==================== BATCH GENERATION TESTS ====================

    @Test
    void testProcessBatchGeneration_Success_Coding() {
        when(redisTemplate.opsForValue()).thenReturn(redisOps);
        when(redisOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(true);
        when(skillViewRepository.findByName("PYTHON")).thenReturn(Optional.of(testSkill));

        taskGenerationService.processBatchGeneration(batchRequest);

        verify(contentGeneratorService, times(1))
                .generateCodingTask(testSkill, TaskDifficulty.INTERMEDIATE, 5);
        verify(replyEventProducer, times(1))
                .publishTaskGenerationSucceeded(any(TaskGenerationSucceededEvent.class));
        verify(redisTemplate, times(1)).delete(anyString());
    }

    @Test
    void testProcessBatchGeneration_Success_Essay() {
        BatchGenerationRequest essayRequest = new BatchGenerationRequest(
                userId, "PYTHON", TaskDifficulty.BEGINNER, 3, TaskType.ESSAY
        );

        when(redisTemplate.opsForValue()).thenReturn(redisOps);
        when(redisOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(true);
        when(skillViewRepository.findByName("PYTHON")).thenReturn(Optional.of(testSkill));

        taskGenerationService.processBatchGeneration(essayRequest);

        verify(contentGeneratorService, times(1))
                .generateEssayTask(testSkill, TaskDifficulty.BEGINNER, 3);
        verify(replyEventProducer, times(1))
                .publishTaskGenerationSucceeded(any(TaskGenerationSucceededEvent.class));
    }

    @Test
    void testProcessBatchGeneration_LockAlreadyAcquired() {
        when(redisTemplate.opsForValue()).thenReturn(redisOps);
        when(redisOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(false);

        taskGenerationService.processBatchGeneration(batchRequest);

        verify(contentGeneratorService, never()).generateCodingTask(any(), any(), anyInt());
        verify(replyEventProducer, times(1))
                .publishTaskGenerationFailed(any(TaskGenerationFailedEvent.class));
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void testProcessBatchGeneration_SkillNotFound() {
        when(redisTemplate.opsForValue()).thenReturn(redisOps);
        when(redisOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(true);
        when(skillViewRepository.findByName("PYTHON")).thenReturn(Optional.empty());

        taskGenerationService.processBatchGeneration(batchRequest);

        verify(contentGeneratorService, never()).generateCodingTask(any(), any(), anyInt());
        verify(replyEventProducer, times(1))
                .publishTaskGenerationFailed(any(TaskGenerationFailedEvent.class));
        verify(redisTemplate, times(1)).delete(anyString());
    }

    @Test
    void testProcessBatchGeneration_GenerationException() {
        when(redisTemplate.opsForValue()).thenReturn(redisOps);
        when(redisOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(true);
        when(skillViewRepository.findByName("PYTHON")).thenReturn(Optional.of(testSkill));
        doThrow(new RuntimeException("AI service error"))
                .when(contentGeneratorService)
                .generateCodingTask(any(), any(), anyInt());

        taskGenerationService.processBatchGeneration(batchRequest);

        verify(replyEventProducer, times(1))
                .publishTaskGenerationFailed(any(TaskGenerationFailedEvent.class));
        verify(redisTemplate, times(1)).delete(anyString());
    }

    @Test
    void testProcessBatchGeneration_SupportsMCQTaskType() throws IOException {
        BatchGenerationRequest mcqRequest = new BatchGenerationRequest(
                userId, "PYTHON", TaskDifficulty.INTERMEDIATE, 5, TaskType.MULTIPLE_CHOICE
        );

        when(redisTemplate.opsForValue()).thenReturn(redisOps);
        when(redisOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(true);
        when(skillViewRepository.findByName("PYTHON")).thenReturn(Optional.of(testSkill));
        when(contentGeneratorService.generateMCQTask(testSkill, TaskDifficulty.INTERMEDIATE, 5))
                .thenReturn(Collections.emptyList());

        taskGenerationService.processBatchGeneration(mcqRequest);

        verify(replyEventProducer, times(1))
                .publishTaskGenerationSucceeded(any(TaskGenerationSucceededEvent.class));
        verify(redisTemplate, times(1)).delete(anyString());
    }

    @Test
    void testProcessBatchGeneration_ReleasesLockOnSuccess() {
        ArgumentCaptor<String> lockKeyCaptor = ArgumentCaptor.forClass(String.class);

        when(redisTemplate.opsForValue()).thenReturn(redisOps);
        when(redisOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(true);
        when(skillViewRepository.findByName("PYTHON")).thenReturn(Optional.of(testSkill));

        taskGenerationService.processBatchGeneration(batchRequest);

        verify(redisTemplate, times(1)).delete(lockKeyCaptor.capture());
        assertTrue(lockKeyCaptor.getValue().contains("lock:task-gen:"));
    }

    @Test
    void testProcessBatchGeneration_PublishesSuccessEvent() {
        when(redisTemplate.opsForValue()).thenReturn(redisOps);
        when(redisOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(true);
        when(skillViewRepository.findByName("PYTHON")).thenReturn(Optional.of(testSkill));

        taskGenerationService.processBatchGeneration(batchRequest);

        ArgumentCaptor<TaskGenerationSucceededEvent> eventCaptor = 
                ArgumentCaptor.forClass(TaskGenerationSucceededEvent.class);
        verify(replyEventProducer, times(1)).publishTaskGenerationSucceeded(eventCaptor.capture());

        assertEquals(userId, eventCaptor.getValue().getUserId());
    }

    @Test
    void testProcessBatchGeneration_PublishesFailureEvent() {
        when(redisTemplate.opsForValue()).thenReturn(redisOps);
        when(redisOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(false);

        taskGenerationService.processBatchGeneration(batchRequest);

        ArgumentCaptor<TaskGenerationFailedEvent> eventCaptor = 
                ArgumentCaptor.forClass(TaskGenerationFailedEvent.class);
        verify(replyEventProducer, times(1)).publishTaskGenerationFailed(eventCaptor.capture());

        assertEquals(userId, eventCaptor.getValue().getUserId());
    }

    // ==================== ADMIN GENERATION TESTS ====================

    @Test
    void testProcessAdminGeneration_Success_Coding() {
        GenerateTaskRequest codingRequest = new GenerateTaskRequest(
                userId, TaskType.CODING, "PYTHON", TaskDifficulty.INTERMEDIATE, "Arrays", "Python"
        );

        when(skillViewRepository.findByName("PYTHON")).thenReturn(Optional.of(testSkill));

        taskGenerationService.processAdminGeneration(codingRequest);

        verify(contentGeneratorService, times(1))
                .generateCodingTask(testSkill, TaskDifficulty.INTERMEDIATE, 1);
        verify(replyEventProducer, times(1))
                .publishTaskGenerationSucceeded(any(TaskGenerationSucceededEvent.class));
    }

    @Test
    void testProcessAdminGeneration_Success_Essay() {
        GenerateTaskRequest essayRequest = new GenerateTaskRequest(
                userId, TaskType.ESSAY, "JAVA", TaskDifficulty.ADVANCED, "Patterns", "Java"
        );

        SkillView javaSkill = new SkillView();
        javaSkill.setId(UUID.randomUUID());
        javaSkill.setName("JAVA");

        when(skillViewRepository.findByName("JAVA")).thenReturn(Optional.of(javaSkill));

        taskGenerationService.processAdminGeneration(essayRequest);

        verify(contentGeneratorService, times(1))
                .generateEssayTask(javaSkill, TaskDifficulty.ADVANCED, 1);
        verify(replyEventProducer, times(1))
                .publishTaskGenerationSucceeded(any(TaskGenerationSucceededEvent.class));
    }

    @Test
    void testProcessAdminGeneration_SkillNotFound() {
        when(skillViewRepository.findByName("UNKNOWN")).thenReturn(Optional.empty());

        GenerateTaskRequest unknownRequest = new GenerateTaskRequest(
                userId, TaskType.CODING, "UNKNOWN", TaskDifficulty.BEGINNER, "Test", "Test"
        );

        taskGenerationService.processAdminGeneration(unknownRequest);

        verify(replyEventProducer, times(1))
                .publishTaskGenerationFailed(any(TaskGenerationFailedEvent.class));
    }

    @Test
    void testProcessAdminGeneration_GenerationException() {
        when(skillViewRepository.findByName("PYTHON")).thenReturn(Optional.of(testSkill));
        doThrow(new RuntimeException("Generation failed"))
                .when(contentGeneratorService)
                .generateEssayTask(any(), any(), anyInt());

        taskGenerationService.processAdminGeneration(adminRequest);

        verify(replyEventProducer, times(1))
                .publishTaskGenerationFailed(any(TaskGenerationFailedEvent.class));
    }

    @Test
    void testProcessAdminGeneration_SupportsMCQTaskType() throws IOException {
        GenerateTaskRequest mcqRequest = new GenerateTaskRequest(
                userId, TaskType.MULTIPLE_CHOICE, "PYTHON", TaskDifficulty.INTERMEDIATE, "Test", "Python"
        );

        when(skillViewRepository.findByName("PYTHON")).thenReturn(Optional.of(testSkill));
        when(contentGeneratorService.generateMCQTask(testSkill, TaskDifficulty.INTERMEDIATE, 1))
                .thenReturn(Collections.emptyList());

        taskGenerationService.processAdminGeneration(mcqRequest);

        verify(replyEventProducer, times(1))
                .publishTaskGenerationSucceeded(any(TaskGenerationSucceededEvent.class));
    }

    @Test
    void testProcessAdminGeneration_GeneratesExactlyOneTask() {
        when(skillViewRepository.findByName("PYTHON")).thenReturn(Optional.of(testSkill));

        taskGenerationService.processAdminGeneration(adminRequest);

        verify(contentGeneratorService, times(1))
                .generateEssayTask(testSkill, TaskDifficulty.ADVANCED, 1);
    }

    // ==================== ONBOARDING GENERATION TESTS ====================

    @Test
    void testGenerateTasksAfterOnboarding_Success() {
        Set<String> taskTypes = new HashSet<>();
        taskTypes.add(TaskType.CODING.name());
        UserOnboardingCompletedEvent.SkillSelectionData skillData = UserOnboardingCompletedEvent.SkillSelectionData.builder()
                .skillId(skillId)
                .skillName("PYTHON")
                .difficultyLevel(TaskDifficulty.INTERMEDIATE.name())
                .supportedTaskTypes(taskTypes)
                .build();
        UserOnboardingCompletedEvent event = UserOnboardingCompletedEvent.builder()
                .userId(userId)
                .selectedSkills(List.of(skillData))
                .build();

        when(skillViewRepository.findById(skillId)).thenReturn(Optional.of(testSkill));
        when(taskRepository.countBySkillAndDifficultyAndType(
                skillId, TaskDifficulty.INTERMEDIATE, TaskType.CODING, true
        )).thenReturn(0L);
        when(userSkillProfileRepository.save(any(UserSkillProfile.class)))
                .thenReturn(new UserSkillProfile());

        taskGenerationService.generateTasksAfterOnboarding(event);

        verify(contentGeneratorService, times(1))
                .generateCodingTask(testSkill, TaskDifficulty.INTERMEDIATE, 5);
        verify(replyEventProducer, times(1))
                .publishTaskGenerationSucceeded(any(TaskGenerationSucceededEvent.class));
    }

    @Test
    void testGenerateTasksAfterOnboarding_SkipIfTasksExist() {
        Set<String> taskTypes = new HashSet<>();
        taskTypes.add(TaskType.CODING.name());
        UserOnboardingCompletedEvent.SkillSelectionData skillData = UserOnboardingCompletedEvent.SkillSelectionData.builder()
                .skillId(skillId)
                .skillName("PYTHON")
                .difficultyLevel(TaskDifficulty.INTERMEDIATE.name())
                .supportedTaskTypes(taskTypes)
                .build();
        UserOnboardingCompletedEvent event = UserOnboardingCompletedEvent.builder()
                .userId(userId)
                .selectedSkills(List.of(skillData))
                .build();

        when(skillViewRepository.findById(skillId)).thenReturn(Optional.of(testSkill));
        when(taskRepository.countBySkillAndDifficultyAndType(
                skillId, TaskDifficulty.INTERMEDIATE, TaskType.CODING, true
        )).thenReturn(10L);
        when(userSkillProfileRepository.save(any(UserSkillProfile.class)))
                .thenReturn(new UserSkillProfile());

        taskGenerationService.generateTasksAfterOnboarding(event);

        verify(contentGeneratorService, never()).generateCodingTask(any(), any(), anyInt());
        verify(replyEventProducer, times(1))
                .publishTaskGenerationSucceeded(any(TaskGenerationSucceededEvent.class));
    }

    @Test
    void testGenerateTasksAfterOnboarding_MultipleSkills() {
        UUID skill2Id = UUID.randomUUID();
        SkillView skill2 = new SkillView();
        skill2.setId(skill2Id);
        skill2.setName("JAVA");

        Set<String> taskTypes1 = new HashSet<>();
        taskTypes1.add(TaskType.CODING.name());
        UserOnboardingCompletedEvent.SkillSelectionData skillData1 = UserOnboardingCompletedEvent.SkillSelectionData.builder()
                .skillId(skillId)
                .skillName("PYTHON")
                .difficultyLevel(TaskDifficulty.INTERMEDIATE.name())
                .supportedTaskTypes(taskTypes1)
                .build();
        
        Set<String> taskTypes2 = new HashSet<>();
        taskTypes2.add(TaskType.ESSAY.name());
        UserOnboardingCompletedEvent.SkillSelectionData skillData2 = UserOnboardingCompletedEvent.SkillSelectionData.builder()
                .skillId(skill2Id)
                .skillName("JAVA")
                .difficultyLevel(TaskDifficulty.BEGINNER.name())
                .supportedTaskTypes(taskTypes2)
                .build();
        UserOnboardingCompletedEvent event = UserOnboardingCompletedEvent.builder()
                .userId(userId)
                .selectedSkills(List.of(skillData1, skillData2))
                .build();

        when(skillViewRepository.findById(skillId)).thenReturn(Optional.of(testSkill));
        when(skillViewRepository.findById(skill2Id)).thenReturn(Optional.of(skill2));
        when(taskRepository.countBySkillAndDifficultyAndType(any(UUID.class), any(), any(), anyBoolean()))
                .thenReturn(0L);
        when(userSkillProfileRepository.save(any(UserSkillProfile.class)))
                .thenReturn(new UserSkillProfile());

        taskGenerationService.generateTasksAfterOnboarding(event);

        verify(contentGeneratorService, times(1))
                .generateCodingTask(testSkill, TaskDifficulty.INTERMEDIATE, 5);
        verify(contentGeneratorService, times(1))
                .generateEssayTask(skill2, TaskDifficulty.BEGINNER, 5);
        verify(replyEventProducer, times(1))
                .publishTaskGenerationSucceeded(any(TaskGenerationSucceededEvent.class));
    }

    @Test
    void testGenerateTasksAfterOnboarding_PartialFailure() {
        Set<String> taskTypes = new HashSet<>();
        taskTypes.add(TaskType.CODING.name());
        UserOnboardingCompletedEvent.SkillSelectionData skillData = UserOnboardingCompletedEvent.SkillSelectionData.builder()
                .skillId(skillId)
                .skillName("PYTHON")
                .difficultyLevel(TaskDifficulty.INTERMEDIATE.name())
                .supportedTaskTypes(taskTypes)
                .build();
        UserOnboardingCompletedEvent event = UserOnboardingCompletedEvent.builder()
                .userId(userId)
                .selectedSkills(List.of(skillData))
                .build();

        when(skillViewRepository.findById(skillId)).thenReturn(Optional.of(testSkill));
        when(taskRepository.countBySkillAndDifficultyAndType(
                skillId, TaskDifficulty.INTERMEDIATE, TaskType.CODING, true
        )).thenReturn(0L);
        doThrow(new RuntimeException("AI service error"))
                .when(contentGeneratorService)
                .generateCodingTask(any(), any(), anyInt());
        when(userSkillProfileRepository.save(any(UserSkillProfile.class)))
                .thenReturn(new UserSkillProfile());

        taskGenerationService.generateTasksAfterOnboarding(event);

        verify(replyEventProducer, times(1))
                .publishTaskGenerationFailed(any(TaskGenerationFailedEvent.class));
    }

    @Test
    void testGenerateTasksAfterOnboarding_SavesUserSkillProfile() {
        Set<String> taskTypes = new HashSet<>();
        taskTypes.add(TaskType.CODING.name());
        UserOnboardingCompletedEvent.SkillSelectionData skillData = UserOnboardingCompletedEvent.SkillSelectionData.builder()
                .skillId(skillId)
                .skillName("PYTHON")
                .difficultyLevel(TaskDifficulty.INTERMEDIATE.name())
                .supportedTaskTypes(taskTypes)
                .build();
        UserOnboardingCompletedEvent event = UserOnboardingCompletedEvent.builder()
                .userId(userId)
                .selectedSkills(List.of(skillData))
                .build();

        when(skillViewRepository.findById(skillId)).thenReturn(Optional.of(testSkill));
        when(taskRepository.countBySkillAndDifficultyAndType(
                skillId, TaskDifficulty.INTERMEDIATE, TaskType.CODING, true
        )).thenReturn(0L);
        when(userSkillProfileRepository.save(any(UserSkillProfile.class)))
                .thenReturn(new UserSkillProfile());

        taskGenerationService.generateTasksAfterOnboarding(event);

        ArgumentCaptor<UserSkillProfile> profileCaptor = 
                ArgumentCaptor.forClass(UserSkillProfile.class);
        verify(userSkillProfileRepository, atLeast(1)).save(profileCaptor.capture());

        UserSkillProfile savedProfile = profileCaptor.getValue();
        assertEquals(userId, savedProfile.getId().getUserId());
        assertEquals(skillId, savedProfile.getId().getSkillId());
        assertEquals("PYTHON", savedProfile.getSkillName());
    }

    @Test
    void testGenerateTasksAfterOnboarding_MultipleTaskTypes() {
        Set<String> taskTypes = new HashSet<>();
        taskTypes.add(TaskType.CODING.name());
        taskTypes.add(TaskType.ESSAY.name());
        UserOnboardingCompletedEvent.SkillSelectionData skillData = UserOnboardingCompletedEvent.SkillSelectionData.builder()
                .skillId(skillId)
                .skillName("PYTHON")
                .difficultyLevel(TaskDifficulty.INTERMEDIATE.name())
                .supportedTaskTypes(taskTypes)
                .build();
        UserOnboardingCompletedEvent event = UserOnboardingCompletedEvent.builder()
                .userId(userId)
                .selectedSkills(List.of(skillData))
                .build();

        when(skillViewRepository.findById(skillId)).thenReturn(Optional.of(testSkill));
        when(taskRepository.countBySkillAndDifficultyAndType(any(UUID.class), any(), any(), anyBoolean()))
                .thenReturn(0L);
        when(userSkillProfileRepository.save(any(UserSkillProfile.class)))
                .thenReturn(new UserSkillProfile());

        taskGenerationService.generateTasksAfterOnboarding(event);

        verify(contentGeneratorService, times(1))
                .generateCodingTask(testSkill, TaskDifficulty.INTERMEDIATE, 5);
        verify(contentGeneratorService, times(1))
                .generateEssayTask(testSkill, TaskDifficulty.INTERMEDIATE, 5);
    }

    @Test
    void testGenerateTasksAfterOnboarding_SkillNotFound() {
        Set<String> taskTypes = new HashSet<>();
        taskTypes.add(TaskType.CODING.name());
        UserOnboardingCompletedEvent.SkillSelectionData skillData = UserOnboardingCompletedEvent.SkillSelectionData.builder()
                .skillId(skillId)
                .skillName("UNKNOWN")
                .difficultyLevel(TaskDifficulty.INTERMEDIATE.name())
                .supportedTaskTypes(taskTypes)
                .build();
        UserOnboardingCompletedEvent event = UserOnboardingCompletedEvent.builder()
                .userId(userId)
                .selectedSkills(List.of(skillData))
                .build();

        when(skillViewRepository.findById(skillId)).thenReturn(Optional.empty());

        taskGenerationService.generateTasksAfterOnboarding(event);

        verify(replyEventProducer, times(1))
                .publishTaskGenerationFailed(any(TaskGenerationFailedEvent.class));
    }

    @Test
    void testGenerateTasksAfterOnboarding_PartialGeneration() {
        Set<String> taskTypes = new HashSet<>();
        taskTypes.add(TaskType.CODING.name());
        UserOnboardingCompletedEvent.SkillSelectionData skillData = UserOnboardingCompletedEvent.SkillSelectionData.builder()
                .skillId(skillId)
                .skillName("PYTHON")
                .difficultyLevel(TaskDifficulty.INTERMEDIATE.name())
                .supportedTaskTypes(taskTypes)
                .build();
        UserOnboardingCompletedEvent event = UserOnboardingCompletedEvent.builder()
                .userId(userId)
                .selectedSkills(List.of(skillData))
                .build();

        when(skillViewRepository.findById(skillId)).thenReturn(Optional.of(testSkill));
        when(taskRepository.countBySkillAndDifficultyAndType(
                skillId, TaskDifficulty.INTERMEDIATE, TaskType.CODING, true
        )).thenReturn(2L);
        when(userSkillProfileRepository.save(any(UserSkillProfile.class)))
                .thenReturn(new UserSkillProfile());

        taskGenerationService.generateTasksAfterOnboarding(event);

        verify(contentGeneratorService, times(1))
                .generateCodingTask(testSkill, TaskDifficulty.INTERMEDIATE, 3);
        verify(replyEventProducer, times(1))
                .publishTaskGenerationSucceeded(any(TaskGenerationSucceededEvent.class));
    }

    // ==================== EVENT PUBLISHING TESTS ====================

    @Test
    void testProcessBatchGeneration_EventContainsUserId() {
        when(redisTemplate.opsForValue()).thenReturn(redisOps);
        when(redisOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(true);
        when(skillViewRepository.findByName("PYTHON")).thenReturn(Optional.of(testSkill));

        taskGenerationService.processBatchGeneration(batchRequest);

        ArgumentCaptor<TaskGenerationSucceededEvent> eventCaptor = 
                ArgumentCaptor.forClass(TaskGenerationSucceededEvent.class);
        verify(replyEventProducer).publishTaskGenerationSucceeded(eventCaptor.capture());

        assertEquals(userId, eventCaptor.getValue().getUserId());
    }

    @Test
    void testProcessAdminGeneration_EventContainsUserId() {
        when(skillViewRepository.findByName("PYTHON")).thenReturn(Optional.of(testSkill));

        taskGenerationService.processAdminGeneration(adminRequest);

        ArgumentCaptor<TaskGenerationSucceededEvent> eventCaptor = 
                ArgumentCaptor.forClass(TaskGenerationSucceededEvent.class);
        verify(replyEventProducer).publishTaskGenerationSucceeded(eventCaptor.capture());

        assertEquals(userId, eventCaptor.getValue().getUserId());
    }

    @Test
    void testGenerateTasksAfterOnboarding_EventContainsUserId() {
        Set<String> taskTypes = new HashSet<>();
        taskTypes.add(TaskType.CODING.name());
        UserOnboardingCompletedEvent.SkillSelectionData skillData = UserOnboardingCompletedEvent.SkillSelectionData.builder()
                .skillId(skillId)
                .skillName("PYTHON")
                .difficultyLevel(TaskDifficulty.INTERMEDIATE.name())
                .supportedTaskTypes(taskTypes)
                .build();
        UserOnboardingCompletedEvent event = UserOnboardingCompletedEvent.builder()
                .userId(userId)
                .selectedSkills(List.of(skillData))
                .build();

        when(skillViewRepository.findById(skillId)).thenReturn(Optional.of(testSkill));
        when(taskRepository.countBySkillAndDifficultyAndType(any(UUID.class), any(), any(), anyBoolean()))
                .thenReturn(0L);
        when(userSkillProfileRepository.save(any(UserSkillProfile.class)))
                .thenReturn(new UserSkillProfile());

        taskGenerationService.generateTasksAfterOnboarding(event);

        ArgumentCaptor<TaskGenerationSucceededEvent> eventCaptor = 
                ArgumentCaptor.forClass(TaskGenerationSucceededEvent.class);
        verify(replyEventProducer).publishTaskGenerationSucceeded(eventCaptor.capture());

        assertEquals(userId, eventCaptor.getValue().getUserId());
    }

    // ==================== QUANTITY TESTS ====================

    @Test
    void testGenerateTasksAfterOnboarding_DifferentQuantitiesPerType() {
        Set<String> taskTypes = new HashSet<>();
        taskTypes.add(TaskType.CODING.name());
        taskTypes.add(TaskType.ESSAY.name());
        taskTypes.add(TaskType.MULTIPLE_CHOICE.name());
        UserOnboardingCompletedEvent.SkillSelectionData skillData = UserOnboardingCompletedEvent.SkillSelectionData.builder()
                .skillId(skillId)
                .skillName("PYTHON")
                .difficultyLevel(TaskDifficulty.INTERMEDIATE.name())
                .supportedTaskTypes(taskTypes)
                .build();
        UserOnboardingCompletedEvent event = UserOnboardingCompletedEvent.builder()
                .userId(userId)
                .selectedSkills(List.of(skillData))
                .build();

        when(skillViewRepository.findById(skillId)).thenReturn(Optional.of(testSkill));
        when(taskRepository.countBySkillAndDifficultyAndType(any(UUID.class), any(), any(), anyBoolean()))
                .thenReturn(0L);
        when(userSkillProfileRepository.save(any(UserSkillProfile.class)))
                .thenReturn(new UserSkillProfile());

        taskGenerationService.generateTasksAfterOnboarding(event);

        verify(contentGeneratorService, times(1))
                .generateCodingTask(testSkill, TaskDifficulty.INTERMEDIATE, 5);
        verify(contentGeneratorService, times(1))
                .generateEssayTask(testSkill, TaskDifficulty.INTERMEDIATE, 5);
    }

    // ==================== DIFFICULTY HANDLING TESTS ====================

    @Test
    void testGenerateTasksAfterOnboarding_AllDifficulties() {
        for (TaskDifficulty difficulty : TaskDifficulty.values()) {
            Set<String> taskTypes = new HashSet<>();
            taskTypes.add(TaskType.CODING.name());
            UserOnboardingCompletedEvent.SkillSelectionData skillData = 
                    UserOnboardingCompletedEvent.SkillSelectionData.builder()
                    .skillId(skillId)
                    .skillName("PYTHON")
                    .difficultyLevel(difficulty.name())
                    .supportedTaskTypes(taskTypes)
                    .build();
            UserOnboardingCompletedEvent event = UserOnboardingCompletedEvent.builder()
                    .userId(userId)
                    .selectedSkills(List.of(skillData))
                    .build();

            when(skillViewRepository.findById(skillId)).thenReturn(Optional.of(testSkill));
            when(taskRepository.countBySkillAndDifficultyAndType(
                    skillId, difficulty, TaskType.CODING, true
            )).thenReturn(0L);
            when(userSkillProfileRepository.save(any(UserSkillProfile.class)))
                    .thenReturn(new UserSkillProfile());

            taskGenerationService.generateTasksAfterOnboarding(event);

            verify(contentGeneratorService, times(1))
                    .generateCodingTask(testSkill, difficulty, 5);
        }
    }
}
