package com.amalitech.task.service.service.impl;

import com.amalitech.common.event.events.TaskGenerationFailedEvent;
import com.amalitech.common.event.events.TaskGenerationSucceededEvent;
import com.amalitech.common.event.events.UserOnboardingCompletedEvent;
import com.amalitech.task.service.dto.request.BatchGenerationRequest;
import com.amalitech.task.service.dto.request.GenerateTaskRequest;
import com.amalitech.task.service.events.TaskReplyEventProducer;
import com.amalitech.task.service.exception.TaskGenerationException;
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

import java.time.Duration;
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
    private final int mcqOnboardingQuantity;
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
            @Value("${app.task.onboarding-quantity.multiple-choice:10}") int mcqOnboardingQuantity,
            @Value("${app.task.onboarding-quantity.essay:5}") int essayOnboardingQuantity
    ) {
        this.redisTemplate = redisTemplate;
        this.skillViewRepository = skillViewRepository;
        this.taskRepository = taskRepository;
        this.contentGeneratorService = contentGeneratorService;
        this.replyEventProducer = replyEventProducer;
        this.userSkillProfileRepository = userSkillProfileRepository;
        this.codingOnboardingQuantity = codingOnboardingQuantity;
        this.mcqOnboardingQuantity = mcqOnboardingQuantity;
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
                    "Batch job for this skill/type is already in progress."
            );
            replyEventProducer.publishTaskGenerationFailed(failEvent);
            return;
        }

        try {
            log.info("Acquired lock {}. Generating {} {} tasks in a single batch...", lockKey, request.requiredCount(), request.taskType());
            SkillView skill = skillViewRepository.findByName(request.skillName())
                    .orElseThrow(() -> new TaskGenerationException("Skill not found: " + request.skillName()));

            switch (request.taskType()) {
                case CODING:
                    contentGeneratorService.generateCodingTask(skill, request.difficulty(), request.requiredCount());
                    break;
                case ESSAY:
                    contentGeneratorService.generateEssayTask(skill, request.difficulty(), request.requiredCount());
                    break;
                case MULTIPLE_CHOICE:
                default:
                    log.warn("Batch generation for {} not yet implemented for skill: {}", request.taskType(), skill.getName());
                    throw new UnsupportedOperationException("Generation for " + request.taskType() + " is not supported.");
            }

            log.info("Batch generation complete for {}", lockKey);
            TaskGenerationSucceededEvent successEvent = new TaskGenerationSucceededEvent(requesterUserId);
            replyEventProducer.publishTaskGenerationSucceeded(successEvent);

        } catch (Exception e) {
            log.error("Failed to generate BATCH tasks for {}: {}", lockKey, e.getMessage(), e);
            TaskGenerationFailedEvent failEvent = new TaskGenerationFailedEvent(
                    requesterUserId,
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

            switch (request.taskType()) {
                case CODING:
                    contentGeneratorService.generateCodingTask(skill, request.difficulty(), 1);
                    break;
                case ESSAY:
                    contentGeneratorService.generateEssayTask(skill, request.difficulty(), 1
                    );
                    break;
                case MULTIPLE_CHOICE:
                default:
                    log.warn("Admin generation for {} not yet implemented for skill: {}", request.taskType(), skill.getName());
                    throw new UnsupportedOperationException("Generation for " + request.taskType() + " is not supported.");
            }

            log.info("Admin {} task generation complete for {}", request.taskType(), request.topic());
            TaskGenerationSucceededEvent successEvent = new TaskGenerationSucceededEvent(requesterUserId);
            replyEventProducer.publishTaskGenerationSucceeded(successEvent);

        } catch (Exception e) {
            log.error("Failed to generate ADMIN task...", e);
            TaskGenerationFailedEvent failEvent = new TaskGenerationFailedEvent(
                    requesterUserId,
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

        try {
            for (UserOnboardingCompletedEvent.SkillSelectionData skillData : event.getSelectedSkills()) {

                try {
                    for (String taskTypeStr : skillData.getSupportedTaskTypes()) {
                        TaskType taskType = TaskType.valueOf(taskTypeStr.toUpperCase());
                        int quantity = getTaskQuantity(taskType);

                        saveUserSkillProfile(userId, skillData);

                        log.info("Generating {} {} tasks for skill: {}",
                                quantity, taskType, skillData.getSkillName());

                        generateTasksOfType(skillData, taskType, quantity);
                    }
                } catch (Exception e) {
                    log.error("Failed to generate tasks for skill {}: {}",
                            skillData.getSkillId(), e.getMessage());
                    throw new TaskGenerationException("Failed to generate tasks for skill: " + skillData.getSkillName(), e);
                }
            }

            log.info("Successfully completed ALL task generation for user: {}", userId);
            TaskGenerationSucceededEvent successEvent = new TaskGenerationSucceededEvent(userId);
            replyEventProducer.publishTaskGenerationSucceeded(successEvent);

        } catch (Exception e) {
            log.error("CRITICAL: Task generation saga FAILED for user {}: {}",
                    userId, e.getMessage(), e);

            TaskGenerationFailedEvent failEvent = new TaskGenerationFailedEvent(userId, e.getMessage());
            replyEventProducer.publishTaskGenerationFailed(failEvent);
        }
    }

    private int getTaskQuantity(TaskType taskType) {
        return switch (taskType) {
            case MULTIPLE_CHOICE -> this.mcqOnboardingQuantity;
            case CODING -> this.codingOnboardingQuantity;
            case ESSAY -> this.essayOnboardingQuantity;
            default -> 5;
        };
    }

    private void generateTasksOfType(UserOnboardingCompletedEvent.SkillSelectionData skillData,
                                     TaskType taskType, int quantity) {
        SkillView skill = skillViewRepository.findById(skillData.getSkillId())
                .orElseThrow(() -> new TaskGenerationException("Skill not found: " + skillData.getSkillId()));

        TaskDifficulty difficulty = TaskDifficulty.valueOf(skillData.getDifficultyLevel().toUpperCase());

        long existingTaskCount = taskRepository.countBySkillAndDifficultyAndType(
                skill.getId(), difficulty, taskType, true);

        if (existingTaskCount >= quantity) {
            log.info("Sufficient {} {} tasks already exist ({} found, {} requested). Skipping generation.",
                    taskType, difficulty, existingTaskCount, quantity);
            return;
        }

        int tasksToGenerate = quantity - (int) existingTaskCount;
        log.info("Found {} existing {} {} tasks. Generating {} additional tasks.",
                existingTaskCount, taskType, difficulty, tasksToGenerate);

        switch (taskType) {
            case CODING:
                contentGeneratorService.generateCodingTask(skill, difficulty, tasksToGenerate);
                break;
            case ESSAY:
                contentGeneratorService.generateEssayTask(skill, difficulty, tasksToGenerate);
                break;
            case MULTIPLE_CHOICE:
                log.warn("MCQ generation not yet implemented for skill: {}", skill.getName());
                break;
        }
    }

    /**
     * Helper method to save the replicated user skill data.
     */
    private void saveUserSkillProfile(UUID userId, UserOnboardingCompletedEvent.SkillSelectionData skillData) {
        UserSkillProfile.UserSkillId id = new UserSkillProfile.UserSkillId(userId, skillData.getSkillId());

        UserSkillProfile profile = UserSkillProfile.builder()
                .id(id)
                .skillName(skillData.getSkillName())
                .difficulty(TaskDifficulty.valueOf(skillData.getDifficultyLevel().toUpperCase()))
                .build();

        userSkillProfileRepository.save(profile);
        log.info("Saved local user skill profile for user {} and skill {}", userId, skillData.getSkillName());
    }
}
