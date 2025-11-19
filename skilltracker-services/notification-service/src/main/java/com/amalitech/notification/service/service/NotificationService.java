package com.amalitech.notification.service.service;

import com.amalitech.common.event.events.SubmissionEvaluatedEvent;
import com.amalitech.common.event.events.SubmissionExecutedEvent;
import com.amalitech.common.event.events.TaskGenerationFailedEvent;
import com.amalitech.common.event.events.TaskGenerationSucceededEvent;

/**
 * Service interface for handling real-time WebSocket notifications.
 * 
 * Responsible for pushing real-time updates to connected users via WebSocket,
 * complementing the persistent notification storage in MongoDB. Implementations
 * of this service should send notifications to specific users based on their
 * WebSocket subscriptions.
 */
public interface NotificationService {

    /**
     * Sends code execution results to a user in real-time via WebSocket.
     * 
     * Triggered when a code submission has completed execution and test results
     * are available. Includes detailed test results, performance metrics, and
     * execution output.
     *
     * @param event The SubmissionExecutedEvent containing execution results,
     *              test outcomes, and performance metrics for the submission
     */
    void sendExecutionResults(SubmissionExecutedEvent event);

    /**
     * Sends evaluation feedback to a user in real-time via WebSocket.
     * 
     * Triggered when a submission has been evaluated and feedback is ready.
     * Includes the score, evaluation status, detailed feedback, and test results.
     *
     * @param event The SubmissionEvaluatedEvent containing evaluation results,
     *              feedback, and scoring information for the submission
     */
    void sendEvaluationFeedback(SubmissionEvaluatedEvent event);

    /**
     * Sends a task generation completion notification to a user via WebSocket.
     * 
     * Triggered when task generation for a user (typically onboarding or bulk generation)
     * completes successfully. Notifies the user that new tasks are available.
     *
     * @param event The TaskGenerationSucceededEvent containing user ID and generated
     *              task information
     */
    void sendTaskGenerationSuccessNotification(TaskGenerationSucceededEvent event);

    /**
     * Sends a task generation failure notification to a user via WebSocket.
     * 
     * Triggered when task generation fails. Notifies the user of the failure
     * and suggests retry options.
     *
     * @param event The TaskGenerationFailedEvent containing user ID and error details
     */
    void sendTaskGenerationFailedNotification(TaskGenerationFailedEvent event);
}
