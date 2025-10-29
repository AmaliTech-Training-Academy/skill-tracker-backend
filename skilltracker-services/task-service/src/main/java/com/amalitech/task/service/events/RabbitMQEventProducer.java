package com.amalitech.task.service.events;

import com.amalitech.common.event.events.SubmissionCreatedEvent;
import com.amalitech.common.event.events.SubmissionEvaluatedEvent;
import com.amalitech.task.service.config.RabbitMQConfig;
import com.amalitech.task.service.dto.request.BatchGenerationRequest;
import com.amalitech.task.service.dto.request.GenerateTaskRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
@RequiredArgsConstructor
@Slf4j
public class RabbitMQEventProducer implements EventProducer {

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
    public void publishSubmissionCreated(SubmissionCreatedEvent submission) {
        try {
            log.info("Publishing submission created event: {}", submission.getSubmissionId());
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.SUBMISSION_EXCHANGE,
                    RabbitMQConfig.SUBMISSION_CREATED_ROUTING_KEY,
                    submission
            );
        } catch (Exception e) {
            log.error("Failed to publish submission created event for ID: {}", submission.getSubmissionId(), e);
        }
    }

    /**
     * Publishes an event when a submission has been evaluated.
     * Consumed by: notification-service
     *
     * @param event The fully evaluated SubmissionEvaluatedEvent.
     */
    public void publishSubmissionEvaluated(SubmissionEvaluatedEvent event) {
        try {
            log.info("Publishing submission evaluated event: {}", event.getSubmissionId());
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.SUBMISSION_EXCHANGE,
                    RabbitMQConfig.SUBMISSION_EVALUATED_ROUTING_KEY,
                    event
            );
        } catch (Exception e) {
            log.error("Failed to publish submission evaluated event for ID: {}", event.getSubmissionId(), e);
        }
    }
}