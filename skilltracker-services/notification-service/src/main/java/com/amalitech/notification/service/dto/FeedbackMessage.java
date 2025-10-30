package com.amalitech.notification.service.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * DTO for sending final evaluation feedback to the client.
 * This message is sent after the submission has been fully graded and feedback generated.
 */
@Data
@Builder
public class FeedbackMessage {
    
    private UUID submissionId;
    private String status;
    private int score;
    private boolean isCorrect;
    private String feedbackType;
    private String overallFeedback;
    private String stdout;
    private String stderr;
    private List<TestResult> testResults;
    private Double avgExecutionTimeMs;
    private Integer avgMemoryUsedKb;

    /**
     * Represents the result of a single test case execution, included as part of the feedback.
     */
    @Data
    @Builder
    public static class TestResult {
        private boolean passed;
        private String input;
        private String expectedOutput;
        private String actualOutput;
        private Long executionTimeMs;
        private Integer memoryUsedKb;
        private String statusDescription;
    }
}
