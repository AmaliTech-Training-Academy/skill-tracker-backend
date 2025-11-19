package com.amalitech.notification.service.service;

import com.amalitech.common.event.events.SubmissionEvaluatedEvent;
import com.amalitech.common.event.events.SubmissionExecutedEvent;
import com.amalitech.common.event.events.TaskGenerationFailedEvent;
import com.amalitech.common.event.events.TaskGenerationSucceededEvent;
import com.amalitech.notification.service.document.NotificationDocument;
import com.amalitech.notification.service.exception.NotificationNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for persisting notifications to MongoDB.
 * 
 * Handles the complete lifecycle of notification documents including:
 * - Retrieval of notifications by user, with filtering and pagination
 * - Notification state management (read/unread, deletion)
 * - Persistence of domain events as notification records
 * 
 * Notifications are stored with rich context data extracted from events,
 * enabling comprehensive notification history and detailed user interactions.
 */
public interface NotificationPersistenceService {

    /**
     * Retrieves all notifications for a user, ordered by creation time (newest first).
     *
     * @param userId The unique identifier of the user
     * @param pageable Pagination parameters for controlling result set size and offset
     * @return A page of NotificationDocument objects for the user, ordered by creation date descending
     */
    Page<NotificationDocument> getNotificationsForUser(UUID userId, Pageable pageable);

    /**
     * Retrieves only unread notifications for a user, ordered by creation time (newest first).
     *
     * @param userId The unique identifier of the user
     * @param pageable Pagination parameters for controlling result set size and offset
     * @return A page of unread NotificationDocument objects for the user, ordered by creation date descending
     */
    Page<NotificationDocument> getUnreadNotificationsForUser(UUID userId, Pageable pageable);

    /**
     * Counts the number of unread notifications for a user.
     * Useful for badge counts and summary displays in the UI.
     *
     * @param userId The unique identifier of the user
     * @return The count of unread notifications for the user
     */
    long getUnreadNotificationCountForUser(UUID userId);

    /**
     * Retrieves a specific notification by its ID, with authorization check.
     * Ensures the notification belongs to the requesting user.
     *
     * @param id The unique identifier of the notification document
     * @param userId The unique identifier of the user requesting the notification
     * @return The NotificationDocument if found and belongs to the user
     * @throws NotificationNotFoundException if the notification is not found or does not belong to the user
     */
    NotificationDocument getNotification(String id, UUID userId);

    /**
     * Marks a specific notification as read.
     * Updates the notification's read status in MongoDB.
     *
     * @param id The unique identifier of the notification document
     * @param userId The unique identifier of the user (for authorization)
     * @return The updated NotificationDocument with read status set to true
     * @throws NotificationNotFoundException if the notification is not found or does not belong to the user
     */
    NotificationDocument markNotificationAsRead(String id, UUID userId);

    /**
     * Deletes a specific notification.
     * Removes the notification document from MongoDB.
     *
     * @param id The unique identifier of the notification document
     * @param userId The unique identifier of the user (for authorization)
     * @throws NotificationNotFoundException if the notification is not found or does not belong to the user
     */
    void deleteNotification(String id, UUID userId);

    /**
     * Marks all notifications as read for a specific user.
     * Bulk update operation for marking the user's notification inbox as read.
     *
     * @param userId The unique identifier of the user
     * @return The count of notifications that were updated to read status
     */
    long markAllNotificationsAsRead(UUID userId);

    /**
     * Persists code execution results as a notification.
     * 
     * Converts a SubmissionExecutedEvent into a notification document with
     * execution details including test results, performance metrics, and output.
     *
     * @param event The SubmissionExecutedEvent containing execution results to persist
     * @throws RuntimeException if persistence fails
     */
    void persistExecutionResults(SubmissionExecutedEvent event);

    /**
     * Persists submission evaluation feedback as a notification.
     * 
     * Converts a SubmissionEvaluatedEvent into a notification document with
     * scoring, feedback content, and evaluation details.
     *
     * @param event The SubmissionEvaluatedEvent containing evaluation feedback to persist
     */
    void persistEvaluationFeedback(SubmissionEvaluatedEvent event);

    /**
     * Persists successful task generation as a notification.
     * 
     * Converts a TaskGenerationSucceededEvent into a notification document with
     * generated task information including skill IDs and generated task IDs.
     *
     * @param event The TaskGenerationSucceededEvent containing task generation success data to persist
     * @throws RuntimeException if persistence fails
     */
    void persistTaskGenerationSuccess(TaskGenerationSucceededEvent event);

    /**
     * Persists failed task generation as a notification.
     * 
     * Converts a TaskGenerationFailedEvent into a notification document with
     * failure information including skill IDs and error messages for traceability.
     *
     * @param event The TaskGenerationFailedEvent containing task generation failure details to persist
     * @throws RuntimeException if persistence fails
     */
    void persistTaskGenerationFailure(TaskGenerationFailedEvent event);
}
