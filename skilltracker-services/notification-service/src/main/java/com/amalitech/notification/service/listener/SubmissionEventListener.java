package com.amalitech.notification.service.listener;

import com.amalitech.common.event.events.SubmissionEvaluatedEvent;
import com.amalitech.common.event.events.SubmissionExecutedEvent;
import com.amalitech.common.event.events.TaskGenerationFailedEvent;
import com.amalitech.common.event.events.TaskGenerationSucceededEvent;
import com.amalitech.notification.service.config.RabbitMQConfig;
import com.amalitech.notification.service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listens for submission-related events from RabbitMQ and triggers notifications.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SubmissionEventListener {

    private final NotificationService notificationService;

    /**
     * Handles the SubmissionExecutedEvent.
     * This method is triggered when a submission has been executed against test cases.
     * It delegates the event to the NotificationService to send real-time results to the user.
     * @param event The event containing the execution results.
     */
    @RabbitListener(queues = RabbitMQConfig.EXECUTED_QUEUE)
    public void handleSubmissionExecuted(SubmissionExecutedEvent event) {
        log.info("Received SubmissionExecutedEvent for submission: {}", event.getSubmissionId());
        
        try {
            notificationService.sendExecutionResults(event);
            log.info("Successfully processed SubmissionExecutedEvent for submission: {}", 
                    event.getSubmissionId());
        } catch (Exception e) {
            log.error("Error processing SubmissionExecutedEvent for submission: {}", 
                    event.getSubmissionId(), e);
        }
    }

    /**
     * Handles the SubmissionEvaluatedEvent.
     * This method is triggered when a submission has been fully evaluated and graded.
     * It delegates the event to the NotificationService to send the final feedback to the user.
     * @param event The event containing the evaluation feedback.
     */
    @RabbitListener(queues = RabbitMQConfig.EVALUATED_QUEUE)
    public void handleSubmissionEvaluated(SubmissionEvaluatedEvent event) {
        log.info("Received SubmissionEvaluatedEvent for submission: {}", event.getSubmissionId());
        
        try {
            notificationService.sendEvaluationFeedback(event);
            log.info("Successfully processed SubmissionEvaluatedEvent for submission: {}", 
                    event.getSubmissionId());
        } catch (Exception e) {
            log.error("Error processing SubmissionEvaluatedEvent for submission: {}",
                    event.getSubmissionId(), e);
        }
    }

    /**
     * Handles the TaskGenerationSucceededEvent.
     * This method is triggered when task generation completes for a user.
     * It notifies the user that new tasks are now available.
     * @param event The event containing task generation completion details.
     */
    @RabbitListener(queues = RabbitMQConfig.TASK_GENERATION_QUEUE)
    public void handleTaskGenerationSucceeded(TaskGenerationSucceededEvent event) {
        log.info("Received TaskGenerationSucceededEvent for user: {}", event.getUserId());

        try {
            notificationService.sendTaskGenerationSuccessNotification(event);
            log.info("Successfully processed TaskGenerationSucceededEvent for user: {}",
                    event.getUserId());
        } catch (Exception e) {
            log.error("Error processing TaskGenerationSucceededEvent for user: {}",
                    event.getUserId(), e);
        }
    }


    /**
     * Handles the TaskGenerationFailedEvent.
     * This method is triggered when task generation fails for a user.
     * It notifies the user that an error occurred.
     * @param event The event containing task generation failure details.
     */
    @RabbitListener(queues = RabbitMQConfig.TASK_GENERATION_FAILED_QUEUE)
    public void handleTaskGenerationFailed(TaskGenerationFailedEvent event) {
        log.info("Received TaskGenerationFailedEvent for user: {}", event.getUserId());

        try {
            notificationService.sendTaskGenerationFailedNotification(event);
            log.info("Successfully processed TaskGenerationFailedEvent for user: {}",
                    event.getUserId());
        } catch (Exception e) {
            log.error("Error processing TaskGenerationFailedEvent for user: {}",
                    event.getUserId(), e);
        }
    }
}
