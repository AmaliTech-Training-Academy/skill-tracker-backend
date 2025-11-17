package com.amalitech.notification.service.service.impl;

import com.amalitech.common.event.events.SubmissionEvaluatedEvent;
import com.amalitech.common.event.events.SubmissionExecutedEvent;
import com.amalitech.common.event.events.TaskGenerationFailedEvent;
import com.amalitech.common.event.events.TaskGenerationSucceededEvent;
import com.amalitech.notification.service.document.NotificationDocument;
import com.amalitech.notification.service.exception.NotificationNotFoundException;
import com.amalitech.notification.service.repository.NotificationRepository;
import com.amalitech.notification.service.service.NotificationPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPersistenceServiceImpl implements NotificationPersistenceService {

    private final NotificationRepository notificationRepository;

    @Override
    public Page<NotificationDocument> getNotificationsForUser(UUID userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Override
    public Page<NotificationDocument> getUnreadNotificationsForUser(UUID userId, Pageable pageable) {
        return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId, pageable);
    }

    @Override
    public long getUnreadNotificationCountForUser(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Override
    public NotificationDocument getNotification(String id, UUID userId) {
        return notificationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotificationNotFoundException(
                        "Notification " + id + " not found or does not belong to user"));
    }

    @Override
    public NotificationDocument markNotificationAsRead(String id, UUID userId) {
        NotificationDocument doc = getNotification(id, userId);
        doc.setRead(true);
        return notificationRepository.save(doc);
    }

    @Override
    public void deleteNotification(String id, UUID userId) {
        NotificationDocument doc = getNotification(id, userId);
        notificationRepository.delete(doc);
    }

    @Override
    public long markAllNotificationsAsRead(UUID userId) {
        long updateCount = notificationRepository.updateAllUnreadToReadByUserId(userId);
        log.info("Marked {} notifications as read for user: {}", updateCount, userId);
        return updateCount;
    }

    @Override
    public void persistExecutionResults(SubmissionExecutedEvent event) {
        try {
            Map<String, Object> context = new HashMap<>();
            context.put("submissionId", event.getSubmissionId());
            context.put("testsPassed", event.getTestsPassed());
            context.put("testsTotal", event.getTestsTotal());

            NotificationDocument notification = NotificationDocument.builder()
                    .userId(event.getUserId())
                    .submissionId(event.getSubmissionId())
                    .type("EXECUTION")
                    .title("Code Execution Results")
                    .message(String.format("Your submission has been executed. %d/%d tests passed.",
                            event.getTestsPassed(), event.getTestsTotal()))
                    .createdAt(LocalDateTime.now())
                    .read(false)
                    .context(context)
                    .build();

            notificationRepository.save(notification);
            log.info("Successfully persisted execution notification for submission: {}", 
                    event.getSubmissionId());
        } catch (Exception e) {
            log.error("Failed to persist execution notification for submission: {}", 
                    event.getSubmissionId(), e);
            throw new RuntimeException("Persistence failed for execution results", e);
        }
    }

    @Override
    public void persistEvaluationFeedback(SubmissionEvaluatedEvent event) {
        try {
            Map<String, Object> context = new HashMap<>();
            context.put("submissionId", event.getSubmissionId());
            context.put("score", event.getScore());
            context.put("isCorrect", event.isCorrect());

            NotificationDocument notification = NotificationDocument.builder()
                    .userId(event.getUserId())
                    .submissionId(event.getSubmissionId())
                    .type("FEEDBACK")
                    .title("Submission Evaluated")
                    .message(String.format("Your submission has been evaluated. Score: %d/100", 
                            event.getScore()))
                    .createdAt(LocalDateTime.now())
                    .read(false)
                    .context(context)
                    .build();

            notificationRepository.save(notification);
            log.info("Successfully persisted feedback notification for submission: {}", 
                    event.getSubmissionId());
        } catch (Exception e) {
            log.error("Failed to persist feedback notification for submission: {}", 
                    event.getSubmissionId(), e);
        }
    }

    @Override
    public void persistTaskGenerationSuccess(TaskGenerationSucceededEvent event) {
        try {
            Map<String, Object> context = new HashMap<>();
            context.put("skillIds", event.getSkillIds());
            context.put("taskCount", event.getGeneratedTaskIds() != null ? event.getGeneratedTaskIds().size() : 0);
            context.put("generatedTaskIds", event.getGeneratedTaskIds());

            NotificationDocument notification = NotificationDocument.builder()
                    .userId(event.getUserId())
                    .type("TASK_GENERATION_SUCCESS")
                    .title("Tasks Generated")
                    .message("New tasks have been generated and are now available for you.")
                    .createdAt(LocalDateTime.now())
                    .read(false)
                    .context(context)
                    .build();

            notificationRepository.save(notification);
            log.info("Successfully persisted task generation success notification for user: {}",
                    event.getUserId());
        } catch (Exception e) {
            log.error("Failed to persist task generation success notification for user: {}",
                    event.getUserId(), e);
            throw new RuntimeException("Persistence failed for task generation success", e);
        }
    }

    @Override
    public void persistTaskGenerationFailure(TaskGenerationFailedEvent event) {
        try {
            Map<String, Object> context = new HashMap<>();
            context.put("skillIds", event.getSkillIds());
            context.put("errorMessage", event.getErrorMessage());

            NotificationDocument notification = NotificationDocument.builder()
                    .userId(event.getUserId())
                    .type("TASK_GENERATION_FAILED")
                    .title("Task Generation Failed")
                    .message("Task generation failed. Please try again later.")
                    .createdAt(LocalDateTime.now())
                    .read(false)
                    .context(context)
                    .build();

            notificationRepository.save(notification);
            log.info("Successfully persisted task generation failure notification for user: {}",
                    event.getUserId());
        } catch (Exception e) {
            log.error("Failed to persist task generation failure notification for user: {}",
                    event.getUserId(), e);
            throw new RuntimeException("Persistence failed for task generation failure", e);
        }
    }
}
