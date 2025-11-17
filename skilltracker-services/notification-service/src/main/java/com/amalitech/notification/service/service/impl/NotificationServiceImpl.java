package com.amalitech.notification.service.service.impl;

import com.amalitech.common.event.events.SubmissionEvaluatedEvent;
import com.amalitech.common.event.events.SubmissionExecutedEvent;
import com.amalitech.common.event.events.TaskGenerationFailedEvent;
import com.amalitech.common.event.events.TaskGenerationSucceededEvent;
import com.amalitech.notification.service.dto.ExecutionResultMessage;
import com.amalitech.notification.service.dto.FeedbackMessage;
import com.amalitech.notification.service.dto.TaskGenerationMessage;
import com.amalitech.notification.service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * Implementation of {@link NotificationService} for real-time WebSocket notifications.
 * 
 * Sends real-time updates to connected users via WebSocket using Spring's
 * STOMP (Simple Text Oriented Messaging Protocol) messaging infrastructure.
 * Each user receives notifications on their personal subscription queues
 * based on their WebSocket connection.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Sends code execution results to a user via WebSocket.
     * 
     * Transforms the event data into an {@link ExecutionResultMessage} and sends it
     * to the user's "/queue/execution" topic. Converts test results and performance
     * metrics into a format suitable for frontend display.
     *
     * @param event The SubmissionExecutedEvent containing execution results
     */
    @Override
    public void sendExecutionResults(SubmissionExecutedEvent event) {
        log.info("Sending execution results to user: {} for submission: {}", 
                event.getUserId(), event.getSubmissionId());

        ExecutionResultMessage message = ExecutionResultMessage.builder()
                .submissionId(event.getSubmissionId())
                .stdout(event.getStdout())
                .stderr(event.getStderr())
                .testResults(event.getTestResults() != null ? 
                        event.getTestResults().stream()
                                .map(tr -> ExecutionResultMessage.TestResult.builder()
                                        .passed(tr.isPassed())
                                        .input(tr.getInput())
                                        .expectedOutput(tr.getExpectedOutput())
                                        .actualOutput(tr.getActualOutput())
                                        .executionTimeMs(tr.getExecutionTimeMs())
                                        .memoryUsedKb(tr.getMemoryUsedKb())
                                        .statusDescription(tr.getStatusDescription())
                                        .build())
                                .collect(Collectors.toList()) : null)
                .allTestsPassed(event.isAllTestsPassed())
                .testsPassed(event.getTestsPassed())
                .testsTotal(event.getTestsTotal())
                .avgExecutionTimeMs(event.getAvgExecutionTimeMs())
                .avgMemoryUsedKb(event.getAvgMemoryUsedKb())
                .build();

        messagingTemplate.convertAndSendToUser(
                event.getUserId().toString(),
                "/queue/execution",
                message
        );

        log.info("Execution results sent successfully to user: {}", event.getUserId());
    }

    /**
     * Sends evaluation feedback to a user via WebSocket.
     * 
     * Transforms the event data into a {@link FeedbackMessage} and sends it to the
     * user's "/queue/feedback" topic. Includes scoring, feedback content, and test
     * result details for the submission.
     *
     * @param event The SubmissionEvaluatedEvent containing evaluation feedback
     */
    @Override
    public void sendEvaluationFeedback(SubmissionEvaluatedEvent event) {
        log.info("Sending evaluation feedback to user: {} for submission: {}", 
                event.getUserId(), event.getSubmissionId());

        FeedbackMessage message = FeedbackMessage.builder()
                .submissionId(event.getSubmissionId())
                .status(event.getStatus())
                .score(event.getScore())
                .isCorrect(event.isCorrect())
                .feedbackType(event.getFeedbackType())
                .overallFeedback(event.getOverallFeedback())
                .stdout(event.getStdout())
                .stderr(event.getStderr())
                .testResults(event.getTestResults() != null ?
                        event.getTestResults().stream()
                                .map(tr -> FeedbackMessage.TestResult.builder()
                                        .passed(tr.isPassed())
                                        .input(tr.getInput())
                                        .expectedOutput(tr.getExpectedOutput())
                                        .actualOutput(tr.getActualOutput())
                                        .executionTimeMs(tr.getExecutionTimeMs())
                                        .memoryUsedKb(tr.getMemoryUsedKb())
                                        .statusDescription(tr.getStatusDescription())
                                        .build())
                                .collect(Collectors.toList()) : null)
                .avgExecutionTimeMs(event.getAvgExecutionTimeMs())
                .avgMemoryUsedKb(event.getAvgMemoryUsedKb())
                .build();

        messagingTemplate.convertAndSendToUser(
                event.getUserId().toString(),
                "/queue/feedback",
                message
        );

        log.info("Evaluation feedback sent successfully to user: {}", event.getUserId());
    }

    /**
     * Sends a task generation success notification to a user via WebSocket.
     * 
     * Transforms the event data into a {@link TaskGenerationMessage} with "COMPLETED"
     * status and sends it to the user's "/queue/tasks" topic. Notifies the user
     * that new tasks are available for completion.
     *
     * @param event The TaskGenerationSucceededEvent containing generation completion data
     */
    @Override
    public void sendTaskGenerationSuccessNotification(TaskGenerationSucceededEvent event) {
        log.info("Sending task generation completion notification to user: {}", event.getUserId());

        TaskGenerationMessage message = TaskGenerationMessage.builder()
                .userId(event.getUserId().toString())
                .status("COMPLETED")
                .message("New tasks have been generated and are now available for you.")
                .completedAt(java.time.LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSendToUser(
                event.getUserId().toString(),
                "/queue/tasks",
                message
        );

        log.info("Task generation notification sent successfully to user: {}", event.getUserId());
    }

    /**
     * Sends a task generation failure notification to a user via WebSocket.
     * 
     * Transforms the event data into a {@link TaskGenerationMessage} with "FAILED"
     * status and sends it to the user's "/queue/tasks" topic. Notifies the user
     * of the failure and suggests retry options.
     *
     * @param event The TaskGenerationFailedEvent containing failure details
     */
    @Override
    public void sendTaskGenerationFailedNotification(TaskGenerationFailedEvent event) {
        log.info("Sending task generation failure notification to user: {}", event.getUserId());

        TaskGenerationMessage message = TaskGenerationMessage.builder()
                .userId(event.getUserId().toString())
                .status("FAILED")
                .message("Task generation failed. Please try again later.")
                .completedAt(java.time.LocalDateTime.now())
                .build();
        messagingTemplate.convertAndSendToUser(
                event.getUserId().toString(),
                "/queue/tasks",
                message
        );

        log.info("Task generation failure notification sent successfully to user: {}", event.getUserId());
    }
}
