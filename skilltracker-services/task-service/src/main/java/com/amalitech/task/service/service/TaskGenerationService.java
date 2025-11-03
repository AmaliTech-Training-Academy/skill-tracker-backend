package com.amalitech.task.service.service;

import com.amalitech.common.event.events.UserOnboardingCompletedEvent;
import com.amalitech.task.service.config.RabbitMQConfig;
import com.amalitech.task.service.dto.request.BatchGenerationRequest;
import com.amalitech.task.service.dto.request.GenerateTaskRequest;
import com.amalitech.task.service.model.enums.TaskDifficulty;
import com.amalitech.task.service.model.enums.TaskType;
import com.amalitech.task.service.model.view.SkillView;
import com.amalitech.task.service.repository.SkillViewRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Orchestrator service for asynchronous task generation.
 *
 * <p>This service acts as the coordinator for AI-powered task content generation,
 * listening to RabbitMQ queues for generation requests and delegating the actual
 * content creation to a {@link ContentGeneratorService}. It implements distributed
 * locking using Redis to prevent duplicate generation attempts for the same
 * skill-difficulty combinations.</p>
 *
 * <p>The service handles two types of generation requests:</p>
 * <ul>
 *   <li><b>Batch Generation</b>: Automatic bulk task creation triggered when task
 *       inventory falls below minimum thresholds. Uses Redis locks to prevent
 *       concurrent duplicate generation (thundering herd prevention).</li>
 *   <li><b>Admin Generation</b>: Manual, on-demand task creation for specific topics
 *       requested by administrators. Does not use locks as these are intentional,
 *       unique requests.</li>
 * </ul>
 *
 * <p><b>Current Limitation:</b> This implementation only supports generating
 * multiple-choice question (MCQ) tasks. Requests for other task types will be
 * logged and skipped.</p>
 *
 * @author AmaliTech
 * @version 1.0
 * @since 1.0
 * @see ContentGeneratorService
 * @see RabbitMQConfig
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TaskGenerationService {

    private final StringRedisTemplate redisTemplate;
    private final SkillViewRepository skillViewRepository;
    private final ContentGeneratorService contentGeneratorService;

    private static final String LOCK_PREFIX = "lock:task-gen:";
    private static final Duration LOCK_TIMEOUT = Duration.ofMinutes(5);

    @RabbitListener(queues = RabbitMQConfig.BATCH_GENERATION_QUEUE)
    public void handleBatchGenerationRequest(BatchGenerationRequest request) {
        log.info("Received BATCH request: {}", request);

        if (request.taskType() != TaskType.CODING) {
            log.warn("Received BATCH request for non-CODING task type: {}. Skipping.", request.taskType());
            return;
        }

        String lockKey = LOCK_PREFIX + request.skillName() + ":" + request.difficulty() + ":" + TaskType.CODING;
        Boolean lockAcquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "in-progress", LOCK_TIMEOUT);

        if (Boolean.FALSE.equals(lockAcquired)) {
            log.warn("Batch job for {} is already in progress. Skipping.", lockKey);
            return;
        }

        try {
            log.info("Acquired lock {}. Generating {} CODING tasks...", lockKey, request.requiredCount());
            SkillView skill = skillViewRepository.findByName(request.skillName())
                    .orElseThrow(() -> new RuntimeException("Skill not found: " + request.skillName()));

            for (int i = 0; i < request.requiredCount(); i++) {
                String topic = String.format("A coding challenge about %s", request.skillName());
                contentGeneratorService.generateCodingTask(skill, request.difficulty());
            }

            log.info("Batch generation complete for {}", lockKey);

        } catch (Exception e) {
            log.error("Failed to generate BATCH CODING tasks for {}: {}", lockKey, e.getMessage(), e);
        } finally {
            Boolean deleted = redisTemplate.delete(lockKey);
            if (deleted) {
                log.info("Released lock {}.", lockKey);
            } else {
                log.warn("Could not release lock {} (may have expired or been deleted).", lockKey);
            }
        }
    }

    @RabbitListener(queues = RabbitMQConfig.ADMIN_GENERATION_QUEUE)
    public void handleAdminGenerationRequest(GenerateTaskRequest request) {
        log.info("Received ADMIN request: {}", request);

        if (request.taskType() != TaskType.CODING) {
            log.warn("Received ADMIN request for non-CODING task type: {}. Skipping.", request.taskType());
            return;
        }

        try {
            SkillView skill = skillViewRepository.findByName(request.skillName())
                    .orElseThrow(() -> new RuntimeException("Skill not found: " + request.skillName()));

            log.info("Generating ADMIN CODING task for topic '{}'...", request.topic());
            contentGeneratorService.generateCodingTask(skill, request.difficulty());

            log.info("Admin CODING task generation complete for {}", request.topic());

        } catch (Exception e) {
            log.error("Failed to generate ADMIN CODING task {}: {}", request, e.getMessage(), e);
        }
    }

    /**
     * Generates personalized tasks for a user who has completed onboarding.
     * Creates tasks based on the supported task types for each selected skill.
     *
     * @param event The onboarding completed event containing user and skill data
     */
    public void generateTasksAfterOnboarding(UserOnboardingCompletedEvent event) {
        log.info("Generating tasks for user onboarding: {}", event.getUserId());

        for (UserOnboardingCompletedEvent.SkillSelectionData skillData : event.getSelectedSkills()) {
            try {
                for (String taskTypeStr : skillData.getSupportedTaskTypes()) {
                    TaskType taskType = TaskType.valueOf(taskTypeStr.toUpperCase());
                    int quantity = getTaskQuantity(taskType);

                    log.info("Generating {} {} tasks for skill: {}",
                            quantity, taskType, skillData.getSkillName());

                    generateTasksOfType(skillData, taskType, quantity);
                }

            } catch (Exception e) {
                log.error("Failed to generate tasks for skill {}: {}",
                         skillData.getSkillId(), e.getMessage());
                // Continue with other skills
            }
        }

        log.info("Completed task generation for user onboarding: {}", event.getUserId());
    }

    private int getTaskQuantity(TaskType taskType) {
        return switch (taskType) {
            case MULTIPLE_CHOICE -> 10;
            case CODING, ESSAY -> 5;
        };
    }

    private void generateTasksOfType(UserOnboardingCompletedEvent.SkillSelectionData skillData,
                                   TaskType taskType, int quantity) {
        SkillView skill = skillViewRepository.findById(skillData.getSkillId())
                .orElseThrow(() -> new RuntimeException("Skill not found: " + skillData.getSkillId()));

        TaskDifficulty difficulty = TaskDifficulty.valueOf(skillData.getDifficultyLevel().toUpperCase());

        for (int i = 0; i < quantity; i++) {
            String topic = String.format("Welcome to %s - Challenge %d",
                    skill.getName(), i + 1);

            switch (taskType) {
                case CODING -> contentGeneratorService.generateCodingTask(skill, difficulty);
                case MULTIPLE_CHOICE -> {
                    // TODO: Implement when MCQ generator is available
                    log.warn("MCQ generation not yet implemented for skill: {}", skill.getName());
                }
                case ESSAY -> {
                    // TODO: Implement when Essay generator is available
                    log.warn("Essay generation not yet implemented for skill: {}", skill.getName());
                }
            }
        }
    }
}