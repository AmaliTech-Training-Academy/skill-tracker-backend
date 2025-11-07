package com.amalitech.notification.service.service;

import com.amalitech.common.event.events.SubmissionEvaluatedEvent;
import com.amalitech.common.event.events.SubmissionExecutedEvent;
import com.amalitech.common.event.events.TaskGenerationSucceededEvent;
import com.amalitech.notification.service.dto.ExecutionResultMessage;
import com.amalitech.notification.service.dto.FeedbackMessage;
import com.amalitech.notification.service.dto.TaskGenerationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * Service responsible for sending real-time notifications to users via WebSockets.
 * It transforms backend events into user-friendly DTOs and sends them to the appropriate user.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Sends the results of a code execution to the user.
     * This is triggered after a submission is run against test cases.
     * @param event The event containing the detailed execution results.
     */
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
     * Sends the final evaluation feedback to the user.
     * This is triggered after a submission has been fully graded.
     * @param event The event containing the score, feedback, and results.
     */
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
     * Sends a notification when task generation completes for a user.
     * This informs the frontend that new tasks are now available.
     * @param event The event containing task generation completion details.
     */
    public void sendTaskGenerationNotification(TaskGenerationSucceededEvent event) {
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
}
