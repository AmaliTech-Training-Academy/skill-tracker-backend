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

    /**
     * Redis key prefix for distributed locks.
     * Lock keys follow the pattern: {@code lock:task-gen:<skillName>:<difficulty>}
     */
    private static final String LOCK_PREFIX = "lock:task-gen:";

    /**
     * Duration for which a Redis lock is held before automatic expiration.
     * This prevents deadlocks if a generation process crashes without releasing the lock.
     */
    private static final Duration LOCK_TIMEOUT = Duration.ofMinutes(5);

    /**
     * Handles batch task generation requests from the RabbitMQ queue.
     *
     * <p>This method processes bulk generation requests that are typically triggered
     * automatically when the system detects insufficient task inventory for a given
     * skill-difficulty combination. It implements distributed locking to ensure only
     * one generation process runs at a time for each unique skill-difficulty pair,
     * preventing resource waste and duplicate task creation.</p>
     *
     * <p><b>Lock Behavior:</b></p>
     * <ul>
     *   <li>Attempts to acquire a Redis lock using the pattern:
     *       {@code lock:task-gen:<skillName>:<difficulty>}</li>
     *   <li>If the lock is already held, the request is skipped (another process
     *       is already generating tasks for this combination)</li>
     *   <li>The lock automatically expires after 5 minutes to prevent deadlocks</li>
     *   <li>The lock is explicitly released in the finally block after generation
     *       completes or fails</li>
     * </ul>
     *
     * <p><b>Generation Process:</b></p>
     * <ol>
     *   <li>Acquires distributed lock for the skill-difficulty combination</li>
     *   <li>Retrieves the skill entity from the database</li>
     *   <li>Generates the requested number of MCQ tasks with generic topics</li>
     *   <li>Releases the lock regardless of success or failure</li>
     * </ol>
     *
     * @param request the batch generation request containing:
     *                <ul>
     *                  <li>{@code skillName} - the skill for which to generate tasks</li>
     *                  <li>{@code difficulty} - the difficulty level of the tasks</li>
     *                  <li>{@code requiredCount} - number of tasks to generate</li>
     *                </ul>
     * @throws RuntimeException if the skill is not found in the database (logged but not propagated)
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
     * Handles administrative task generation requests from the RabbitMQ queue.
     *
     * <p>This method processes manual, on-demand generation requests typically
     * initiated by administrators or system operators for specific topics or use cases.
     * Unlike batch generation, admin requests do not use distributed locking because
     * they represent intentional, specific requests that should always be processed.</p>
     *
     * <p><b>Request Validation:</b></p>
     * <ul>
     *   <li>Only {@code TaskType.MULTIPLE_CHOICE} requests are processed</li>
     *   <li>Requests for other task types are logged and skipped</li>
     *   <li>This reflects the current system limitation to MCQ generation only</li>
     * </ul>
     *
     * <p><b>Generation Process:</b></p>
     * <ol>
     *   <li>Validates the task type (must be MULTIPLE_CHOICE)</li>
     *   <li>Retrieves the skill entity from the database</li>
     *   <li>Generates a single MCQ task for the specified topic</li>
     *   <li>Logs completion or any errors encountered</li>
     * </ol>
     *
     * <p><b>No Lock Required:</b> Admin requests are not deduplicated because each
     * request is assumed to be a distinct, intentional action by an administrator,
     * even if multiple requests happen to target the same skill-difficulty-topic
     * combination.</p>
     *
     * @param request the admin generation request containing:
     *                <ul>
     *                  <li>{@code skillName} - the skill for which to generate the task</li>
     *                  <li>{@code difficulty} - the difficulty level of the task</li>
     *                  <li>{@code taskType} - the type of task (must be MULTIPLE_CHOICE)</li>
     *                  <li>{@code topic} - the specific topic or subject for the question</li>
     *                </ul>
     * @throws RuntimeException if the skill is not found in the database (logged but not propagated)
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