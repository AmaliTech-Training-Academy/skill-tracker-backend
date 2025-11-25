package com.amalitech.task.service.service.impl;

import com.amalitech.common.event.events.TaskGenerationFailedEvent;
import com.amalitech.common.event.events.TaskGenerationSucceededEvent;
import com.amalitech.common.event.events.UserOnboardingCompletedEvent;
import com.amalitech.task.service.dto.request.BatchGenerationRequest;
import com.amalitech.task.service.dto.request.GenerateTaskRequest;
import com.amalitech.task.service.events.TaskReplyEventProducer;
import com.amalitech.task.service.exception.TaskGenerationException;
import com.amalitech.task.service.model.Task;
import com.amalitech.task.service.model.UserSkillProfile;
import com.amalitech.task.service.model.enums.TaskDifficulty;
import com.amalitech.task.service.model.enums.TaskType;
import com.amalitech.task.service.model.view.SkillView;
import com.amalitech.task.service.repository.SkillViewRepository;
import com.amalitech.task.service.repository.TaskRepository;
import com.amalitech.task.service.repository.UserSkillProfileRepository;
import com.amalitech.task.service.service.ContentGeneratorService;
import com.amalitech.task.service.service.TaskGenerationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Asynchronous worker service for task generation.
 * This service contains the @Async and @Transactional business logic,
 * decoupled from the RabbitMQ listeners.
 */
@Service
@Slf4j
public class TaskGenerationServiceImpl implements TaskGenerationService {

    private final StringRedisTemplate redisTemplate;
    private final SkillViewRepository skillViewRepository;
    private final TaskRepository taskRepository;
    private final ContentGeneratorService contentGeneratorService;
    private final TaskReplyEventProducer replyEventProducer;
    private final UserSkillProfileRepository userSkillProfileRepository;

    private final int codingOnboardingQuantity;
    private final int mcqOnboardingTaskQuantity;
    private final int mcqOnboardingQuestionsPerTask;
    private final int essayOnboardingQuantity;

    private static final String LOCK_PREFIX = "lock:task-gen:";
    private static final Duration LOCK_TIMEOUT = Duration.ofMinutes(5);

    public TaskGenerationServiceImpl(
            StringRedisTemplate redisTemplate,
            SkillViewRepository skillViewRepository,
            TaskRepository taskRepository,
            ContentGeneratorService contentGeneratorService,
            TaskReplyEventProducer replyEventProducer,
            UserSkillProfileRepository userSkillProfileRepository,
            @Value("${app.task.onboarding-quantity.coding:5}") int codingOnboardingQuantity,
            @Value("${app.task.onboarding-quantity.multiple-choice-tasks:5}") int mcqOnboardingTaskQuantity,
            @Value("${app.task.onboarding-quantity.multiple-choice-questions:10}") int mcqOnboardingQuestionsPerTask,
            @Value("${app.task.onboarding-quantity.essay:5}") int essayOnboardingQuantity
    ) {
        this.redisTemplate = redisTemplate;
        this.skillViewRepository = skillViewRepository;
        this.taskRepository = taskRepository;
        this.contentGeneratorService = contentGeneratorService;
        this.replyEventProducer = replyEventProducer;
        this.userSkillProfileRepository = userSkillProfileRepository;
        this.codingOnboardingQuantity = codingOnboardingQuantity;
        this.mcqOnboardingTaskQuantity = mcqOnboardingTaskQuantity;
        this.mcqOnboardingQuestionsPerTask = mcqOnboardingQuestionsPerTask;
        this.essayOnboardingQuantity = essayOnboardingQuantity;
    }

    /**
     * @Async worker for BATCH requests.
     * This now correctly runs on a separate thread AND in a transaction.
     */
    @Async
    @Transactional
    @Override
    public void processBatchGeneration(BatchGenerationRequest request) {
        UUID requesterUserId = request.userId();

        String lockKey = LOCK_PREFIX + request.skillName() + ":" + request.difficulty() + ":" + request.taskType();
        Boolean lockAcquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "in-progress", LOCK_TIMEOUT);

        if (Boolean.FALSE.equals(lockAcquired)) {
            log.warn("Batch job for {} is already in progress. Skipping.", lockKey);

            TaskGenerationFailedEvent failEvent = new TaskGenerationFailedEvent(
                    requesterUserId,
                    List.of(),
                    "Batch job for this skill/type is already in progress."
            );
            replyEventProducer.publishTaskGenerationFailed(failEvent);
            return;
        }

        try {
            log.info("Acquired lock {}. Generating {} {} tasks in a single batch...", lockKey, request.requiredCount(), request.taskType());
            SkillView skill = skillViewRepository.findByName(request.skillName())
                    .orElseThrow(() -> new TaskGenerationException("Skill not found: " + request.skillName()));

            List<UUID> generatedTaskIds = new ArrayList<>();

            switch (request.taskType()) {
                case CODING:
                    var codingTasks = contentGeneratorService.generateCodingTask(skill, request.difficulty(), request.requiredCount());
                    generatedTaskIds.addAll(codingTasks.stream().map(Task::getId).toList());
                    break;
                case ESSAY:
                    var essayTasks = contentGeneratorService.generateEssayTask(skill, request.difficulty(), request.requiredCount());
                    generatedTaskIds.addAll(essayTasks.stream().map(Task::getId).toList());
                    break;
                case MULTIPLE_CHOICE:
                    var mcqTasks = contentGeneratorService.generateMCQTask(skill, request.difficulty(), request.requiredCount());
                    generatedTaskIds.addAll(mcqTasks.stream().map(Task::getId).toList());
                    break;
                default:
                    log.warn("Batch generation for {} not yet implemented for skill: {}", request.taskType(), skill.getName());
                    throw new UnsupportedOperationException("Generation for " + request.taskType() + " is not supported.");
            }

            log.info("Batch generation complete for {}", lockKey);
            TaskGenerationSucceededEvent successEvent = new TaskGenerationSucceededEvent(
                    requesterUserId,
                    List.of(skill.getId()),
                    generatedTaskIds
            );
            replyEventProducer.publishTaskGenerationSucceeded(successEvent);

        } catch (Exception e) {
            log.error("Failed to generate BATCH tasks for {}: {}", lockKey, e.getMessage(), e);
            SkillView skill = skillViewRepository.findByName(request.skillName()).orElse(null);
            TaskGenerationFailedEvent failEvent = new TaskGenerationFailedEvent(
                    requesterUserId,
                    skill != null ? List.of(skill.getId()) : List.of(),
                    "Batch generation failed: " + e.getMessage()
            );
            replyEventProducer.publishTaskGenerationFailed(failEvent);

        } finally {
            redisTemplate.delete(lockKey);
            log.info("Released lock {}.", lockKey);
        }
    }

    /**
     * @Async worker for ADMIN requests.
     */
    @Async
    @Transactional
    @Override
    public void processAdminGeneration(GenerateTaskRequest request) {
        UUID requesterUserId = request.userId();

        try {
            SkillView skill = skillViewRepository.findByName(request.skillName())
                    .orElseThrow(() -> new TaskGenerationException("Skill not found: " + request.skillName()));

            log.info("Generating ADMIN {} task (1) for topic '{}'...", request.taskType(), request.topic());

            List<UUID> generatedTaskIds = new ArrayList<>();

            switch (request.taskType()) {
                case CODING:
                    var codingTasks = contentGeneratorService.generateCodingTask(skill, request.difficulty(), 1);
                    generatedTaskIds.addAll(codingTasks.stream().map(Task::getId).toList());
                    break;
                case ESSAY:
                    var essayTasks = contentGeneratorService.generateEssayTask(skill, request.difficulty(), 1);
                    generatedTaskIds.addAll(essayTasks.stream().map(Task::getId).toList());
                    break;
                case MULTIPLE_CHOICE:
                    var mcqTasks = contentGeneratorService.generateMCQTask(skill, request.difficulty(), 1);
                    generatedTaskIds.addAll(mcqTasks.stream().map(Task::getId).toList());
                    break;
                default:
                    log.warn("Admin generation for {} not yet implemented for skill: {}", request.taskType(), skill.getName());
                    throw new UnsupportedOperationException("Generation for " + request.taskType() + " is not supported.");
            }

            log.info("Admin {} task generation complete for {}", request.taskType(), request.topic());
            TaskGenerationSucceededEvent successEvent = new TaskGenerationSucceededEvent(
                    requesterUserId,
                    List.of(skill.getId()),
                    generatedTaskIds
            );
            replyEventProducer.publishTaskGenerationSucceeded(successEvent);

        } catch (Exception e) {
            log.error("Failed to generate ADMIN task...", e);
            SkillView skill = skillViewRepository.findByName(request.skillName()).orElse(null);
            TaskGenerationFailedEvent failEvent = new TaskGenerationFailedEvent(
                    requesterUserId,
                    skill != null ? List.of(skill.getId()) : List.of(),
                    "Admin generation failed: " + e.getMessage()
            );
            replyEventProducer.publishTaskGenerationFailed(failEvent);
        }
    }

    /**
     * This method now treats the generation of ALL tasks for a user
     * as a single atomic operation. The try...catch block wraps the
     * entire loop to ensure that a single failure rolls back the
     * entire onboarding saga.
     */
    @Async
    @Transactional
    @Override
    public void generateTasksAfterOnboarding(UserOnboardingCompletedEvent event) {
        UUID userId = event.getUserId();
        log.info("Generating tasks for user onboarding: {}", userId);

        List<UUID> skillIds = event.getSelectedSkills().stream()
                .map(UserOnboardingCompletedEvent.SkillSelectionData::getSkillId)
                .toList();
        List<UUID> generatedTaskIds = new java.util.ArrayList<>();

        try {
            for (UserOnboardingCompletedEvent.SkillSelectionData skillData : event.getSelectedSkills()) {

                try {
                    for (String taskTypeStr : skillData.getSupportedTaskTypes()) {
                        TaskType taskType = TaskType.valueOf(taskTypeStr.toUpperCase());
                        int quantity = getTaskQuantity(taskType);

                        saveUserSkillProfile(userId, skillData);

                        log.info("Generating {} {} tasks for skill: {}",
                                quantity, taskType, skillData.getSkillName());

                        List<UUID> taskIds = generateTasksOfType(skillData, taskType, quantity);
                        generatedTaskIds.addAll(taskIds);
                    }
                } catch (Exception e) {
                    log.error("Failed to generate tasks for skill {}: {}",
                            skillData.getSkillId(), e.getMessage());
                    throw new TaskGenerationException("Failed to generate tasks for skill: " + skillData.getSkillName(), e);
                }
            }

            log.info("Successfully completed ALL task generation for user: {}. Generated {} tasks", userId, generatedTaskIds.size());
            TaskGenerationSucceededEvent successEvent = new TaskGenerationSucceededEvent(userId, skillIds, generatedTaskIds);
            replyEventProducer.publishTaskGenerationSucceeded(successEvent);

        } catch (Exception e) {
            log.error("CRITICAL: Task generation saga FAILED for user {}: {}",
                    userId, e.getMessage(), e);

            TaskGenerationFailedEvent failEvent = new TaskGenerationFailedEvent(userId, skillIds, e.getMessage());
            replyEventProducer.publishTaskGenerationFailed(failEvent);
        }
    }

    private int getTaskQuantity(TaskType taskType) {
        return switch (taskType) {
            case MULTIPLE_CHOICE -> this.mcqOnboardingTaskQuantity;
            case CODING -> this.codingOnboardingQuantity;
            case ESSAY -> this.essayOnboardingQuantity;
            default -> 5;
        };
    }

    private int getQuestionsPerMcqTask() {
        return this.mcqOnboardingQuestionsPerTask;
    }

    private List<UUID> generateTasksOfType(UserOnboardingCompletedEvent.SkillSelectionData skillData,
                                           TaskType taskType, int quantity) throws IOException {
        SkillView skill = skillViewRepository.findById(skillData.getSkillId())
                .orElseThrow(() -> new TaskGenerationException("Skill not found: " + skillData.getSkillId()));

        TaskDifficulty difficulty = TaskDifficulty.valueOf(skillData.getDifficultyLevel().toUpperCase());

        long existingTaskCount = taskRepository.countBySkillAndDifficultyAndType(
                skill.getId(), difficulty, taskType, true);

        if (existingTaskCount >= quantity) {
            log.info("Sufficient {} {} tasks already exist ({} found, {} requested). Skipping generation.",
                    taskType, difficulty, existingTaskCount, quantity);
            return List.of();
        }

        int tasksToGenerate = quantity - (int) existingTaskCount;
        log.info("Found {} existing {} {} tasks. Generating {} additional tasks.",
                existingTaskCount, taskType, difficulty, tasksToGenerate);

        switch (taskType) {
            case CODING:
                var codingTasks = contentGeneratorService.generateCodingTask(skill, difficulty, tasksToGenerate);
                return codingTasks.stream().map(Task::getId).toList();
            case ESSAY:
                var essayTasks = contentGeneratorService.generateEssayTask(skill, difficulty, tasksToGenerate);
                return essayTasks.stream().map(Task::getId).toList();
            case MULTIPLE_CHOICE:
                return generateMCQTasks(skill, difficulty, tasksToGenerate);
            default:
                return List.of();
        }
    }

    /**
     * Generates multiple MCQ tasks for a skill at a given difficulty level.
     * Each task contains the same number of questions (questionsPerTask).
     * 
     * @param skill the skill to generate MCQ tasks for
     * @param difficulty the difficulty level
     * @param tasksToGenerate the number of MCQ tasks to create
     * @return list of generated task IDs
     */
    private List<UUID> generateMCQTasks(SkillView skill, TaskDifficulty difficulty, int tasksToGenerate) throws IOException {
        List<UUID> taskIds = new ArrayList<>();
        int questionsPerTask = getQuestionsPerMcqTask();
        
        for (int i = 0; i < tasksToGenerate; i++) {
            try {
                log.info("Generating MCQ task {}/{} for skill {} at {} difficulty with {} questions",
                        i + 1, tasksToGenerate, skill.getName(), difficulty, questionsPerTask);
                
                List<Task> generatedTasks = contentGeneratorService.generateMCQTask(skill, difficulty, questionsPerTask);
                taskIds.addAll(generatedTasks.stream().map(Task::getId).toList());
                
            } catch (Exception e) {
                log.error("Failed to generate MCQ task {}/{} for skill {}: {}",
                        i + 1, tasksToGenerate, skill.getName(), e.getMessage(), e);
                throw e;
            }
        }
        
        return taskIds;
    }

    /**
     * Helper method to save the replicated user skill data.
     */
    private void saveUserSkillProfile(UUID userId, UserOnboardingCompletedEvent.SkillSelectionData skillData) {
        UserSkillProfile.UserSkillId id = new UserSkillProfile.UserSkillId(userId, skillData.getSkillId());

        UserSkillProfile profile = UserSkillProfile.builder()
                .id(id)
                .skillName(skillData.getSkillName().toUpperCase())
                .difficulty(TaskDifficulty.valueOf(skillData.getDifficultyLevel().toUpperCase()))
                .build();

        userSkillProfileRepository.save(profile);
        log.info("Saved local user skill profile for user {} and skill {}", userId, skillData.getSkillName());
    }
}
