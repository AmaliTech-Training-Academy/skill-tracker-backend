package com.amalitech.task.service.service;

import com.amalitech.common.event.events.UserOnboardingCompletedEvent;
import com.amalitech.task.service.config.RabbitMQConfig;
import com.amalitech.task.service.dto.request.BatchGenerationRequest;
import com.amalitech.task.service.dto.request.GenerateTaskRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

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
public class TaskGenerationListener {

    private final TaskGenerationService taskGenerationService;

    @RabbitListener(queues = RabbitMQConfig.BATCH_GENERATION_QUEUE)
    public void handleBatchGenerationRequest(BatchGenerationRequest request) {
        log.info("Received BATCH request: {}. Delegating to async worker.", request);

        taskGenerationService.processBatchGeneration(request);
    }

    @RabbitListener(queues = RabbitMQConfig.ADMIN_GENERATION_QUEUE)
    public void handleAdminGenerationRequest(GenerateTaskRequest request) {
        log.info("Received ADMIN request: {}. Delegating to async worker.", request);

        taskGenerationService.processAdminGeneration(request);
    }

    /**
     * This method is called by UserOnboardingEventConsumer.
     * It just passes the call directly to the worker.
     */
    public void generateTasksAfterOnboarding(UserOnboardingCompletedEvent event) {
        taskGenerationService.generateTasksAfterOnboarding(event);
    }
}