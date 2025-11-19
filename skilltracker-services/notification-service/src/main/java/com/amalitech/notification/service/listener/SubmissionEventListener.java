package com.amalitech.notification.service.listener;

import com.amalitech.common.event.events.SubmissionEvaluatedEvent;
import com.amalitech.common.event.events.SubmissionExecutedEvent;
import com.amalitech.common.event.events.TaskGenerationFailedEvent;
import com.amalitech.common.event.events.TaskGenerationSucceededEvent;
import com.amalitech.notification.service.config.RabbitMQConfig;
import com.amalitech.notification.service.service.NotificationPersistenceService;
import com.amalitech.notification.service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Event listener for notification-related domain events from RabbitMQ.
 * 
 * Orchestrates the complete notification lifecycle by implementing a "Save First, Then Push" pattern:
 * 1. Persists notification data to MongoDB for durability
 * 2. Sends real-time notifications via WebSocket for immediate user feedback
 * 
 * Handles four event types:
 * - {@link SubmissionExecutedEvent} - Code submission execution results
 * - {@link SubmissionEvaluatedEvent} - Submission evaluation and scoring feedback
 * - {@link TaskGenerationSucceededEvent} - Successful task generation notifications
 * - {@link TaskGenerationFailedEvent} - Failed task generation error notifications
 * 
 * All listeners validate event payloads and reject invalid messages to prevent data corruption.
 * Processing errors trigger automatic retries via RabbitMQ's dead letter exchanges.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SubmissionEventListener {

    private final NotificationService notificationService;
    private final NotificationPersistenceService notificationPersistenceService;

    /**
     * Handles code submission execution results.
     * 
     * Processes execution events by:
     * 1. Validating event payload (userId and submissionId must be present)
     * 2. Persisting execution results to MongoDB
     * 3. Sending real-time notification to connected clients
     * 
     * Invalid or incomplete events are rejected immediately to prevent data inconsistency.
     * Processing failures trigger automatic retry via RabbitMQ.
     *
     * @param event The submission execution event containing test results and performance metrics
     * @throws AmqpRejectAndDontRequeueException if event payload is invalid or persistence fails
     */
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

    /**
     * Handles submission evaluation feedback notifications.
     * 
     * Processes evaluation events by:
     * 1. Validating event payload (userId and submissionId must be present)
     * 2. Persisting evaluation feedback to MongoDB
     * 3. Sending real-time feedback notification to connected clients
     * 
     * Invalid events are rejected immediately to prevent data inconsistency.
     * Processing failures trigger automatic retry via RabbitMQ.
     *
     * @param event The submission evaluated event containing score, feedback, and correctness status
     * @throws AmqpRejectAndDontRequeueException if event payload is invalid or persistence fails
     */
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

    /**
     * Handles successful task generation notifications.
     * 
     * Processes task generation success events by:
     * 1. Validating event payload (userId, skillIds, and generatedTaskIds must be present)
     * 2. Persisting task generation success to MongoDB
     * 3. Sending real-time notification to inform user of available tasks
     * 
     * Invalid events with missing skillIds or generatedTaskIds are rejected immediately.
     * Processing failures trigger automatic retry via RabbitMQ.
     *
     * @param event The task generation succeeded event containing generated task IDs and related skill IDs
     * @throws AmqpRejectAndDontRequeueException if event payload is invalid or persistence fails
     */
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

    /**
     * Handles failed task generation error notifications.
     * 
     * Processes task generation failure events by:
     * 1. Validating event payload (userId, skillIds, and errorMessage must be present)
     * 2. Persisting failure notification to MongoDB for audit trail
     * 3. Sending real-time error notification to inform user of generation failure
     * 
     * Invalid events with missing skillIds or errorMessage are rejected immediately.
     * Processing failures trigger automatic retry via RabbitMQ.
     *
     * @param event The task generation failed event containing skill IDs and error message details
     * @throws AmqpRejectAndDontRequeueException if event payload is invalid or persistence fails
     */
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

    /**
     * Validates a SubmissionEvaluatedEvent for required fields.
     * 
     * Ensures the event contains all mandatory data:
     * - event object is not null
     * - userId is present
     * - submissionId is present
     *
     * @param event The event to validate
     * @return true if event is valid and contains all required fields, false otherwise
     */
    private boolean isValid(SubmissionEvaluatedEvent event) {
        return event != null &&
                event.getUserId() != null &&
                event.getSubmissionId() != null;
    }

    /**
     * Validates a TaskGenerationSucceededEvent for required fields.
     * 
     * Ensures the event contains all mandatory data:
     * - event object is not null
     * - userId is present
     * - skillIds list is present and not empty
     * - generatedTaskIds list is present
     *
     * @param event The event to validate
     * @return true if event is valid and contains all required fields, false otherwise
     */
    private boolean isValidTaskGenerationSuccess(TaskGenerationSucceededEvent event) {
        return event != null &&
                event.getUserId() != null &&
                event.getSkillIds() != null &&
                !event.getSkillIds().isEmpty() &&
                event.getGeneratedTaskIds() != null;
    }

    /**
     * Validates a TaskGenerationFailedEvent for required fields.
     * 
     * Ensures the event contains all mandatory data:
     * - event object is not null
     * - userId is present
     * - skillIds list is present
     * - errorMessage is present and not blank
     *
     * @param event The event to validate
     * @return true if event is valid and contains all required fields, false otherwise
     */
    private boolean isValidTaskGenerationFailure(TaskGenerationFailedEvent event) {
        return event != null &&
                event.getUserId() != null &&
                event.getSkillIds() != null &&
                event.getErrorMessage() != null &&
                !event.getErrorMessage().isBlank();
    }
}