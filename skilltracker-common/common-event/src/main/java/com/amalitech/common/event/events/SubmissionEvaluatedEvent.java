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
    
    /**
     * Unique identifier of the user who submitted the solution.
     * Used for WebSocket routing and notifications.
     */
    private UUID userId;
    
    /**
     * Evaluation status.
     * Possible values: "COMPLETED", "ERROR"
     */
    private String status;
    
    /**
     * Score achieved (0-100 scale).
     */
    private int score;
    
    /**
     * Whether the submission is correct (all tests passed).
     */
    private boolean isCorrect;

    /**
     * Type of feedback provided.
     * Possible values: "CODING", "ESSAY", "MCQ"
     */
    private String feedbackType;
    
    /**
     * AI-generated overall feedback and suggestions.
     * Contains comprehensive evaluation from the AI model.
     */
    private String overallFeedback;
    
    /**
     * Standard output from code execution (for CODING tasks).
     * Contains the actual output produced by running the user's code.
     */
    private String stdout;
    
    /**
     * Standard error from code execution (for CODING tasks).
     * Contains error messages, runtime errors, or compilation errors.
     */
    private String stderr;
    
    /**
     * Structured test case results with detailed information.
     * Each result includes pass/fail status, inputs, outputs, and execution time.
     */
    private List<TestResultData> testResults;
    
    /**
     * Average execution time across all test cases in milliseconds.
     */
    private Double avgExecutionTimeMs;
    
    /**
     * Average memory usage across all test cases in kilobytes.
     */
    private Integer avgMemoryUsedKb;

    /**
     * Detailed information about a single test case execution result.
     */
    @Data
    @Builder
    public static class TestResultData {
        
        /**
         * Whether this test case passed.
         */
        private boolean passed;
        
        /**
         * Input provided to the test case.
         */
        private String input;
        
        /**
         * Expected output for this test case.
         */
        private String expectedOutput;
        
        /**
         * Actual output produced by the user's code.
         */
        private String actualOutput;
        
        /**
         * Execution time for this specific test case in milliseconds.
         */
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