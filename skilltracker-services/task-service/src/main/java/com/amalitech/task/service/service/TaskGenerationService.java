package com.amalitech.task.service.service;

import com.amalitech.task.service.config.RabbitMQConfig;
import com.amalitech.task.service.dto.request.BatchGenerationRequest;
import com.amalitech.task.service.dto.request.GenerateTaskRequest;
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
 * Orchestrator service for task generation.
 * Listens to RabbitMQ queues, handles Redis locking for deduplication,
 * and delegates the actual content generation to an AiContentGenerator.
 *
 * This implementation is limited to generating MCQ tasks only.
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

    /**
     * LISTENER 1: For USER "batch" requests (MCQ-only)
     * USES THE REDIS LOCK to prevent thundering herds.
     */
    @RabbitListener(queues = RabbitMQConfig.BATCH_GENERATION_QUEUE)
    public void handleBatchGenerationRequest(BatchGenerationRequest request) {
        log.info("Received BATCH request: {}", request);

        String lockKey = LOCK_PREFIX + request.skillName() + ":" + request.difficulty();
        Boolean lockAcquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "in-progress", LOCK_TIMEOUT);

        if (Boolean.FALSE.equals(lockAcquired)) {
            log.warn("Batch job for {} is already in progress. Skipping.", lockKey);
            return;
        }

        try {
            log.info("Acquired lock {}. Generating {} MCQ tasks...", lockKey, request.requiredCount());
            SkillView skill = skillViewRepository.findByName(request.skillName())
                    .orElseThrow(() -> new RuntimeException("Skill not found: " + request.skillName()));

            for (int i = 0; i < request.requiredCount(); i++) {
                String topic = String.format("A question about %s fundamentals", request.skillName());

                contentGeneratorService.generateMcqTask(skill, request.difficulty(), topic);
            }

            log.info("Batch generation complete for {}", lockKey);

        } catch (Exception e) {
            log.error("Failed to generate BATCH tasks for {}: {}", lockKey, e.getMessage(), e);
        } finally {
            redisTemplate.delete(lockKey);
            log.info("Released lock {}.", lockKey);
        }
    }

    /**
     * LISTENER 2: For ADMIN "special order" requests (MCQ-only)
     * This DOES NOT NEED A LOCK, as it's a specific, manual action.
     */
    @RabbitListener(queues = RabbitMQConfig.ADMIN_GENERATION_QUEUE)
    public void handleAdminGenerationRequest(GenerateTaskRequest request) {
        log.info("Received ADMIN request: {}", request);

        if (request.taskType() != TaskType.MULTIPLE_CHOICE) {
            log.warn("Received ADMIN request for non-MCQ task type: {}. " +
                    "This service only supports MULTIPLE_CHOICE. Skipping.", request.taskType());
            return;
        }

        try {
            SkillView skill = skillViewRepository.findByName(request.skillName())
                    .orElseThrow(() -> new RuntimeException("Skill not found: " + request.skillName()));

            log.info("Generating ADMIN MCQ...");

            contentGeneratorService.generateMcqTask(skill, request.difficulty(), request.topic());

            log.info("Admin task generation complete for {}", request.topic());

        } catch (Exception e) {
            log.error("Failed to generate ADMIN task {}: {}", request, e.getMessage(), e);
        }
    }
}