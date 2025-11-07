package com.amalitech.common.event.events;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * Event published when a user submits a task solution for evaluation.
 * 
 * <p>This event is published by the task-service and consumed by the feedback-service
 * to trigger automatic evaluation of the submission. The event contains all necessary
 * information for the feedback-service to evaluate the submission without needing to
 * call back to the task-service.</p>
 * 
 * <p>Different task types use different fields:</p>
 * <ul>
 *   <li><strong>CODING tasks:</strong> Use codeToEvaluate, languageId, and testCases</li>
 *   <li><strong>ESSAY tasks:</strong> Use essayToEvaluate</li>
 *   <li><strong>MCQ tasks:</strong> Use taskType only (answer checked directly)</li>
 * </ul>
 * 
 * @see SubmissionEvaluatedEvent
 */
@Data
@Builder
public class SubmissionCreatedEvent {
    
    /**
     * Unique identifier of the submission being evaluated.
     */
    private UUID submissionId;
    
    /**
     * Unique identifier of the user who submitted the solution.
     * Used for routing evaluation results back to the correct user.
     */
    private UUID userId;
    
    /**
     * Unique identifier of the task being attempted.
     */
    private UUID taskId;

    /**
     * Type of task being evaluated.
     * Possible values: "CODING", "MCQ", "ESSAY"
     * 
     * <p>The feedback-service uses this to select the appropriate
     * evaluation strategy (TaskEvaluator implementation).</p>
     */
    private String taskType;
    
    /**
     * Content submitted by the user for evaluation.
     * The content type depends on the task type:
     * - CODING tasks: Contains the source code
     * - ESSAY tasks: Contains the written response
     * - MCQ tasks: Contains the selected answer
     */
    private String contentToEvaluate;

    /**
     * Judge0 language identifier for code execution.
     * Examples:
     * <ul>
     *   <li>62 - Java</li>
     *   <li>71 - Python</li>
     *   <li>63 - JavaScript (Node.js)</li>
     *   <li>54 - C++</li>
     * </ul>
     *
     * <p>Only required for CODING tasks. Null for other types.</p>
     *
     * @see <a href="https://ce.judge0.com/languages">Judge0 Language List</a>
     */
    private Integer languageId;

    /**
     * List of test cases to execute against the submitted code.
     * Each test case contains input and expected output for validation.
     *
     * <p>Only populated for CODING tasks. Empty or null for other types.</p>
     */
    private List<TestCaseData> testCases;

    /**
     * Name of the skill associated with this task.
     * Used for contextual AI evaluation.
     * <p>Populated for all task types to provide evaluation context.</p>
     */
    private String skillName;

    /**
     * Difficulty level of the task.
     * Used for contextual AI evaluation.
     * <p>Populated for all task types to provide evaluation context.</p>
     */
    private String difficulty;

    /**
     * Title of the task.
     * Used for contextual AI evaluation.
     * <p>Populated for all task types to provide evaluation context.</p>
     */
    private String taskTitle;

    /**
     * Description/prompt of the task.
     * Used for contextual AI evaluation.
     * <p>Populated for all task types to provide evaluation context.</p>
     */
    private String taskDescription;

    /**
     * Detailed instructions for the task.
     * Used for contextual AI evaluation.
     * <p>Only populated for ESSAY task types. Null for other types.</p>
     */
    private String detailedInstructions;

    /**
     * Evaluation criteria for the task.
     * Used by AI to understand how to evaluate submissions.
     * <p>Only populated for ESSAY task types. Null for other types.</p>
     */
    private String evaluationCriteria;

    /**
     * Grading rubric for the task.
     * Used by AI to understand performance levels.
     * <p>Only populated for ESSAY task types. Null for other types.</p>
     */
    private String rubric;

    /**
     * Test case data containing input and expected output for code validation.
     * 
     * <p>Test cases can be visible (shown to user) or hidden (used for grading only).
     * The feedback-service executes the submitted code with each test case's input
     * and compares the actual output with the expected output.</p>
     */
    @Data
    @Builder
    public static class TestCaseData {
        
        /**
         * Input data to pass to the program during execution.
         * For example: "hello world" for a string reversal function.
         * 
         * <p>This value is passed as stdin to the Judge0 execution environment.</p>
         */
        private String input;
        
        /**
         * Expected output that the correct solution should produce.
         * Used to validate if the user's code produces correct results.
         * 
         * <p>The feedback-service compares this with the actual output
         * from code execution to determine if the test passed.</p>
         */
        private String expectedOutput;
    }
}