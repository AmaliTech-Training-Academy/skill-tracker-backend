package com.amalitech.common.event.events;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Event published when a user completes a task (after evaluation).
 *
 * <p>This event contains comprehensive information about task completion including
 * user ID, skill ID, task details, XP earned, pass/fail status, and detailed rubric scores.
 * It is published by the task-service after receiving evaluation results and is
 * consumed by analytics services to track user progress and learning metrics.</p>
 * 
 * <p><strong>Rubric Scoring:</strong></p>
 * <ul>
 *   <li>For CODING tasks, rubric scores are extracted from: correctness (max 50), efficiency (max 30), style (max 20).
 *   <li>For ESSAY tasks, rubric scores are extracted from: completeness (max 25), accuracy (max 30), 
 *       clarity (max 25), depth (max 20).
 *   <li>Each rubric score contains: score (weighted points as Double), maxScore (maximum points), 
 *       percentage (0-100% achievement).
 * </ul>
 * 
 * <p>The <code>totalXpEarned</code> is calculated based on the overall submission percentage score
 * (typically: XP = (percentage / 100) × maxXP), independent of individual rubric scores.</p>
 */
@Data
@Builder
public class TaskCompletedEvent {

    /**
     * Unique identifier of the user who completed the task.
     */
    private UUID userId;

    /**
     * Unique identifier of the skill associated with this task.
     */
    private UUID skillId;

    /**
     * Unique identifier of the task.
     */
    private String taskId;

    /**
     * Description of the task.
     */
    private String taskDescription;

    /**
     * Total XP (experience points) earned by the user for completing this task.
     * 
     * <p>Calculated as: (overall_percentage / 100) × task_max_xp</p>
     */
    private Integer totalXpEarned;

    /**
     * Whether the user passed the task (true) or failed (false).
     * Typically determined by: overall_percentage >= 70%.
     */
    private Boolean passed;

    /**
     * Timestamp when the task was completed (when evaluation was finalized).
     */
    private Instant completedAt;

    /**
     * Type of the task (e.g., CODING, MCQ, ESSAY, PROJECT).
     */
    private String taskType;

    /**
     * Difficulty level of the task (e.g., EASY, MEDIUM, HARD).
     */
    private String taskDifficulty;

    /**
     * Map of rubric scores indexed by rubric name.
     * 
     * <p>Keys are rubric category names (e.g., "correctness", "efficiency", "style" for coding tasks;
     * "completeness", "accuracy", "clarity", "depth" for essay tasks).
     * Values contain a detailed score breakdown including weighted points and achievement percentage.</p>
     */
    private Map<String, RubricScoreData> rubricsScores;

    /**
     * DTO for individual rubric scores.
     */
    @Data
    @Builder
    public static class RubricScoreData {
        /**
         * Score achieved for this rubric (as a floating-point to preserve precision).
         * Example: 45.5 for a score of 45.5 points
         */
        private Double score;

        /**
         * Maximum possible score for this rubric.
         */
        private Integer maxScore;

        /**
         * Percentage score (score / maxScore * 100).
         */
        private Integer percentage;
    }
}
