package com.amalitech.common.event.events;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * Event published immediately after code execution completes (before AI evaluation).
 * 
 * <p>This event provides fast feedback to users, showing them code execution results
 * within 2-3 seconds. It contains stdout/stderr and test results but does NOT include
 * AI-generated feedback (which takes longer).</p>
 * 
 * <p>Flow:</p>
 * <ol>
 *   <li>User submits code</li>
 *   <li>Feedback-service executes code via Judge0 (fast)</li>
 *   <li>Publishes this event → User sees output immediately</li>
 *   <li>Then generates AI feedback (slower)</li>
 *   <li>Publishes SubmissionEvaluatedEvent → User sees AI feedback</li>
 * </ol>
 * 
 * @see SubmissionEvaluatedEvent
 */
@Data
@Builder
public class SubmissionExecutedEvent {
    
    /**
     * Unique identifier of the submission.
     */
    private UUID submissionId;
    
    /**
     * Unique identifier of the user (for WebSocket routing).
     */
    private UUID userId;
    
    /**
     * Standard output from code execution.
     * What the user's code printed/produced.
     */
    private String stdout;
    
    /**
     * Standard error from code execution.
     * Contains runtime errors, exceptions, or compilation errors.
     */
    private String stderr;
    
    /**
     * Detailed results for each test case executed.
     */
    private List<TestResultData> testResults;
    
    /**
     * Whether all test cases passed.
     */
    private boolean allTestsPassed;
    
    /**
     * Number of test cases that passed.
     */
    private int testsPassed;
    
    /**
     * Total number of test cases executed.
     */
    private int testsTotal;
    
    /**
     * Average execution time across all test cases in milliseconds.
     */
    private Double avgExecutionTimeMs;
    
    /**
     * Average memory usage across all test cases in kilobytes.
     */
    private Integer avgMemoryUsedKb;

    /**
     * Detailed result for a single test case execution.
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
         * Execution time for this test case in milliseconds.
         */
        private Long executionTimeMs;
        
        /**
         * Memory used for this test case in kilobytes.
         */
        private Integer memoryUsedKb;
        
        /**
         * Judge0 status description (e.g., "Accepted", "Wrong Answer").
         */
        private String statusDescription;
    }
}
