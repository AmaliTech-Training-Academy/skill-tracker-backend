package com.amalitech.task.service.model.enums;

/**
 * Enumeration defining the possible states in the lifecycle of a user's task submission
 * within the SkillBoost platform.
 * <p>
 * These statuses track a submission from its initial receipt through the asynchronous
 * AI evaluation process to its final state, facilitating clear state management and
 * user experience updates.
 * <ul>
 * <li>{@code PENDING}: The submission has been received by the Task Service but the evaluation event has not yet been processed by the Evaluation Service.</li>
 * <li>{@code EVALUATING}: The Evaluation Service (AI Agent) has picked up the submission and is currently running the scoring logic.</li>
 * <li>{@code COMPLETED}: The evaluation is finished, and the submission record has been updated with the final score, feedback, and result.</li>
 * <li>{@code ERROR}: An unrecoverable error occurred during the submission or evaluation process (e.g., bad data, system failure).</li>
 * </ul>
 */
public enum SubmissionStatus {
    PENDING,
    EVALUATING,
    COMPLETED,
    ERROR
}