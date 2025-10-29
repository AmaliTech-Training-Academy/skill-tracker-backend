package com.amalitech.task.service.events;

import com.amalitech.task.service.config.RabbitMQConfig;
import com.amalitech.task.service.dto.request.BatchGenerationRequest;
import com.amalitech.task.service.dto.request.GenerateTaskRequest;
import com.amalitech.task.service.model.TaskSubmission;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskEventProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * This is for the USER flow (batch restock)
     */
    public void requestBatchTaskGeneration(BatchGenerationRequest request) {
        try {
            log.info("Publishing BATCH generation request: {}", request);
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.BATCH_GENERATION_QUEUE,
                    request
            );
        } catch (Exception e) {
            log.error("Failed to publish BATCH generation request", e);
        }
    }

    /**
     * Request admin task generation (manual, no lock needed)
     */
    public void requestSpecificTaskGeneration(GenerateTaskRequest request) {
        try {
            log.info("Publishing ADMIN generation request: {}", request);
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ADMIN_GENERATION_QUEUE,
                    request
            );
        } catch (Exception e) {
            log.error("Failed to publish ADMIN generation request", e);
        }
    }

    /**
     * Publishes an event when a new submission is created.
     * Consumed by: evaluation-service
     *
     * @param submission The TaskSubmission object to be evaluated.
     */
    public void publishSubmissionCreated(TaskSubmission submission) {
        try {
            log.info("Publishing submission created event: {}", submission.getId());
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.SUBMISSION_EXCHANGE,
                    RabbitMQConfig.SUBMISSION_CREATED_ROUTING_KEY,
                    submission
            );
        } catch (Exception e) {
            log.error("Failed to publish submission created event for ID: {}", submission.getId(), e);
        }
    }

    /**
     * Publishes an event when a submission has been evaluated.
     * Consumed by: notification-service
     *
     * @param submission The fully evaluated TaskSubmission object.
     */
    public void publishSubmissionEvaluated(TaskSubmission submission) {
        try {
            log.info("Publishing submission evaluated event: {}", submission.getId());
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.SUBMISSION_EXCHANGE,
                    RabbitMQConfig.SUBMISSION_EVALUATED_ROUTING_KEY,
                    submission
            );
        } catch (Exception e) {
            log.error("Failed to publish submission evaluated event for ID: {}", submission.getId(), e);
        }
    }
}