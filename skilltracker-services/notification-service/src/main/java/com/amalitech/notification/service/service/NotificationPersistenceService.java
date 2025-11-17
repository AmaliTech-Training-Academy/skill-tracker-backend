package com.amalitech.notification.service.service;

import com.amalitech.common.event.events.SubmissionEvaluatedEvent;
import com.amalitech.common.event.events.SubmissionExecutedEvent;
import com.amalitech.common.event.events.TaskGenerationFailedEvent;
import com.amalitech.common.event.events.TaskGenerationSucceededEvent;
import com.amalitech.notification.service.document.NotificationDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NotificationPersistenceService {

    Page<NotificationDocument> getNotificationsForUser(UUID userId, Pageable pageable);

    Page<NotificationDocument> getUnreadNotificationsForUser(UUID userId, Pageable pageable);

    long getUnreadNotificationCountForUser(UUID userId);

    NotificationDocument getNotification(String id, UUID userId);

    NotificationDocument markNotificationAsRead(String id, UUID userId);

    void deleteNotification(String id, UUID userId);

    long markAllNotificationsAsRead(UUID userId);

    void persistExecutionResults(SubmissionExecutedEvent event);

    void persistEvaluationFeedback(SubmissionEvaluatedEvent event);

    void persistTaskGenerationSuccess(TaskGenerationSucceededEvent event);

    void persistTaskGenerationFailure(TaskGenerationFailedEvent event);
}
