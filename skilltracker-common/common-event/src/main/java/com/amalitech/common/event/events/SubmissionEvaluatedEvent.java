package com.amalitech.common.event.events;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;
import java.util.List;

/**
 * Event published when a submission has been evaluated by the feedback-service.
 * 
 * <p>This event contains the complete evaluation results including code execution output,
 * test results, and AI-generated feedback. It is consumed by the task-service to update
 * the submission record and can be used by notification services for real-time updates.</p>
 * 
 * @see SubmissionCreatedEvent
 */
@Data
@Builder
public class SubmissionEvaluatedEvent {
    
    /**
     * Unique identifier of the evaluated submission.
     */
    private UUID submissionId;

    private UUID userId;
    private String status;
    private int score;
    private boolean isCorrect;
    private String feedbackType;
    private String overallFeedback;
    private String stdout;
    private String stderr;
    private List<TestResultData> testResults;
    private Double avgExecutionTimeMs;
    private Integer avgMemoryUsedKb;

    @Data
    @Builder
    public static class TestResultData {
        private boolean passed;
        private String input;
        private String expectedOutput;
        private String actualOutput;
        private Long executionTimeMs;
        
        /**
         * Memory used for this specific test case in kilobytes.
         */
        private Integer memoryUsedKb;
        
        /**
         * Judge0 status description (e.g., "Accepted", "Wrong Answer", "Runtime Error").
         */
        private String statusDescription;
    }
}