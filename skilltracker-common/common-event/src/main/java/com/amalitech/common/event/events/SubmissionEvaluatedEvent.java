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
 * <p><strong>Score Semantics:</strong></p>
 * <ul>
 *   <li><code>score</code>: Percentage-based score (0-100) representing overall performance.
 *       For coding tasks, this is the percentage of passed tests (0% = no tests passed, 100% = all tests passed).
 *       For essay tasks, this is the weighted score from AI evaluation (e.g., 83% based on completeness, accuracy, clarity, depth).
 *   </li>
 *   <li><code>isCorrect</code>: Boolean indicating if the submission meets the passing threshold (typically score >= 70%).
 *   </li>
 * </ul>
 * 
 * <p><code>detailedFeedback</code> JSON structure includes rubric scores with <code>score</code> fields representing
 * weighted points (not percentages). For example, in coding tasks: correctness.score is out of 50, efficiency.score
 * is out of 30, style.score is out of 20.</p>
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
    
    /**
     * Overall score as a percentage (0-100).
     * 
     * <p>For CODING tasks: Percentage of test cases passed (0 = no tests passed, 100 = all tests passed).
     * For ESSAY tasks: Weighted score from AI evaluation based on rubric categories.
     * </p>
     */
    private int score;
    
    /**
     * Whether the submission meets the passing threshold (typically score >= 70%).
     */
    private boolean isCorrect;
    
    private String feedbackType;
    private String overallFeedback;
    
    /**
     * Detailed feedback as JSON string containing rubric-based evaluation.
     * 
     * <p>Structure varies by task type:
     * <ul>
     *   <li>CODING: Contains evaluation.correctness, evaluation.efficiency, evaluation.style
     *       with scores as weighted points (max 50, 30, 20 respectively).
     *   </li>
     *   <li>ESSAY: Contains evaluation.completeness, evaluation.accuracy, evaluation.clarity, evaluation.depth
     *       with scores as weighted points (max 25, 30, 25, 20 respectively).
     *   </li>
     * </ul>
     * </p>
     */
    private String detailedFeedback;
    
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