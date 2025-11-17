package com.amalitech.notification.service.service;

import com.amalitech.common.event.events.SubmissionEvaluatedEvent;
import com.amalitech.common.event.events.SubmissionExecutedEvent;
import com.amalitech.common.event.events.TaskGenerationFailedEvent;
import com.amalitech.common.event.events.TaskGenerationSucceededEvent;
import com.amalitech.notification.service.document.NotificationDocument;
import com.amalitech.notification.service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPersistenceService {

    private final NotificationRepository notificationRepository;

    @Async
    public void persistExecutionResults(SubmissionExecutedEvent event) {
        try {
            NotificationDocument notification = NotificationDocument.builder()
                    .userId(event.getUserId())
                    .submissionId(event.getSubmissionId())
                    .type("EXECUTION")
                    .title("Code Execution Results")
                    .message(String.format("Your submission has been executed. %d/%d tests passed.",
                            event.getTestsPassed(), event.getTestsTotal()))
                    .createdAt(LocalDateTime.now())
                    .read(false)
                    .data(event)
                    .build();

            notificationRepository.save(notification);
            log.info("Successfully persisted execution notification for submission: {}", 
                    event.getSubmissionId());
        } catch (Exception e) {
            log.error("Failed to persist execution notification for submission: {}", 
                    event.getSubmissionId(), e);
        }
    }

    @Async
    public void persistEvaluationFeedback(SubmissionEvaluatedEvent event) {
        try {
            NotificationDocument notification = NotificationDocument.builder()
                    .userId(event.getUserId())
                    .submissionId(event.getSubmissionId())
                    .type("FEEDBACK")
                    .title("Submission Evaluated")
                    .message(String.format("Your submission has been evaluated. Score: %d/100", 
                            event.getScore()))
                    .createdAt(LocalDateTime.now())
                    .read(false)
                    .data(event)
                    .build();

            notificationRepository.save(notification);
            log.info("Successfully persisted feedback notification for submission: {}", 
                    event.getSubmissionId());
        } catch (Exception e) {
            log.error("Failed to persist feedback notification for submission: {}", 
                    event.getSubmissionId(), e);
        }
    }

    @Async
    public void persistTaskGenerationSuccess(TaskGenerationSucceededEvent event) {
        try {
            NotificationDocument notification = NotificationDocument.builder()
                    .userId(event.getUserId())
                    .type("TASK_GENERATION_SUCCESS")
                    .title("Tasks Generated")
                    .message("New tasks have been generated and are now available for you.")
                    .createdAt(LocalDateTime.now())
                    .read(false)
                    .data(event)
                    .build();

            notificationRepository.save(notification);
            log.info("Successfully persisted task generation success notification for user: {}", 
                    event.getUserId());
        } catch (Exception e) {
            log.error("Failed to persist task generation success notification for user: {}", 
                    event.getUserId(), e);
        }
    }

    @Async
    public void persistTaskGenerationFailure(TaskGenerationFailedEvent event) {
        try {
            NotificationDocument notification = NotificationDocument.builder()
                    .userId(event.getUserId())
                    .type("TASK_GENERATION_FAILED")
                    .title("Task Generation Failed")
                    .message("Task generation failed. Please try again later.")
                    .createdAt(LocalDateTime.now())
                    .read(false)
                    .data(event)
                    .build();

            notificationRepository.save(notification);
            log.info("Successfully persisted task generation failure notification for user: {}", 
                    event.getUserId());
        } catch (Exception e) {
            log.error("Failed to persist task generation failure notification for user: {}", 
                    event.getUserId(), e);
        }
    }
}
