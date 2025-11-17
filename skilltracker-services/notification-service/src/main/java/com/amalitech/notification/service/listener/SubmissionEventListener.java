package com.amalitech.notification.service.listener;

import com.amalitech.common.event.events.SubmissionEvaluatedEvent;
import com.amalitech.common.event.events.SubmissionExecutedEvent;
import com.amalitech.common.event.events.TaskGenerationFailedEvent;
import com.amalitech.common.event.events.TaskGenerationSucceededEvent;
import com.amalitech.notification.service.config.RabbitMQConfig;
import com.amalitech.notification.service.service.NotificationPersistencePort;
import com.amalitech.notification.service.service.INotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listens for events from RabbitMQ and orchestrates persistence and real-time pushing.
 * Implements a "Save First, Then Push" pattern for data consistency.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SubmissionEventListener {

    private final INotificationService notificationService;
    private final NotificationPersistencePort notificationPersistenceService;

    @RabbitListener(queues = RabbitMQConfig.EXECUTED_QUEUE)
    public void handleSubmissionExecuted(SubmissionExecutedEvent event) {
        log.info("Received SubmissionExecutedEvent for submission: {}", event.getSubmissionId());

        if (event.getUserId() == null || event.getSubmissionId() == null) {
            log.error("Invalid SubmissionExecutedEvent received: {}. Rejecting message.", event);
            throw new AmqpRejectAndDontRequeueException("Invalid event payload");
        }

        try {
            notificationPersistenceService.persistExecutionResults(event);

            notificationService.sendExecutionResults(event);

            log.info("Successfully processed SubmissionExecutedEvent for submission: {}",
                    event.getSubmissionId());
        } catch (Exception e) {
            log.error("Error processing SubmissionExecutedEvent for submission: {}. " +
                    "Event will be retried.", event.getSubmissionId(), e);
            throw new AmqpRejectAndDontRequeueException("Persistence failed, will retry", e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.EVALUATED_QUEUE)
    public void handleSubmissionEvaluated(SubmissionEvaluatedEvent event) {
        log.info("Received SubmissionEvaluatedEvent for submission: {}", event.getSubmissionId());

        if (!isValid(event)) {
            log.error("Invalid SubmissionEvaluatedEvent received: {}. Rejecting message.", event);
            throw new AmqpRejectAndDontRequeueException("Invalid event payload");
        }

        try {
            notificationPersistenceService.persistEvaluationFeedback(event);

            notificationService.sendEvaluationFeedback(event);

            log.info("Successfully processed SubmissionEvaluatedEvent for submission: {}",
                    event.getSubmissionId());
        } catch (Exception e) {
            log.error("Error processing SubmissionEvaluatedEvent for submission: {}. " +
                    "Event will be retried.", event.getSubmissionId(), e);
            throw new AmqpRejectAndDontRequeueException("Persistence failed, will retry", e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.TASK_GENERATION_QUEUE)
    public void handleTaskGenerationSucceeded(TaskGenerationSucceededEvent event) {
        log.info("Received TaskGenerationSucceededEvent for user: {}", event.getUserId());

        if (!isValidTaskGenerationSuccess(event)) {
            log.error("Invalid TaskGenerationSucceededEvent received: {}. Rejecting message.", event);
            throw new AmqpRejectAndDontRequeueException("Invalid event payload: missing skillIds or generatedTaskIds");
        }

        try {
            notificationPersistenceService.persistTaskGenerationSuccess(event);

            notificationService.sendTaskGenerationSuccessNotification(event);

            log.info("Successfully processed TaskGenerationSucceededEvent for user: {}",
                    event.getUserId());
        } catch (Exception e) {
            log.error("Error processing TaskGenerationSucceededEvent for user: {}. " +
                    "Event will be retried.", event.getUserId(), e);
            throw new AmqpRejectAndDontRequeueException("Persistence failed, will retry", e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.TASK_GENERATION_FAILED_QUEUE)
    public void handleTaskGenerationFailed(TaskGenerationFailedEvent event) {
        log.info("Received TaskGenerationFailedEvent for user: {}", event.getUserId());

        if (!isValidTaskGenerationFailure(event)) {
            log.error("Invalid TaskGenerationFailedEvent received: {}. Rejecting message.", event);
            throw new AmqpRejectAndDontRequeueException("Invalid event payload: missing skillIds or errorMessage");
        }

        try {
            notificationPersistenceService.persistTaskGenerationFailure(event);

            notificationService.sendTaskGenerationFailedNotification(event);

            log.info("Successfully processed TaskGenerationFailedEvent for user: {}",
                    event.getUserId());
        } catch (Exception e) {
            log.error("Error processing TaskGenerationFailedEvent for user: {}. " +
                    "Event will be retried.", event.getUserId(), e);
            throw new AmqpRejectAndDontRequeueException("Persistence failed, will retry", e);
        }
    }

    private boolean isValid(SubmissionEvaluatedEvent event) {
        return event != null &&
                event.getUserId() != null &&
                event.getSubmissionId() != null;
    }

    private boolean isValidTaskGenerationSuccess(TaskGenerationSucceededEvent event) {
        return event != null &&
                event.getUserId() != null &&
                event.getSkillIds() != null &&
                !event.getSkillIds().isEmpty() &&
                event.getGeneratedTaskIds() != null;
    }

    private boolean isValidTaskGenerationFailure(TaskGenerationFailedEvent event) {
        return event != null &&
                event.getUserId() != null &&
                event.getSkillIds() != null &&
                event.getErrorMessage() != null &&
                !event.getErrorMessage().isBlank();
    }
}