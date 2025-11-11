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
 * user ID, skill ID, task details, XP earned, pass/fail status, and rubric scores.
 * It is published by the task-service after receiving evaluation results and is
 * consumed by analytics services to track user progress and learning metrics.</p>
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
     */
    private Integer totalXpEarned;

    /**
     * Whether the user passed the task (true) or failed (false).
     */
    private Boolean passed;

    /**
     * Timestamp when the task was completed.
     */
    private Instant completedAt;

    /**
     * Type of the task (CODING, MCQ, ESSAY, etc.).
     */
    private String taskType;

    /**
     * Difficulty level of the task.
     */
    private String taskDifficulty;

    /**
     * Map of rubric scores where key is rubric name and value is the score details.
     */
    private Map<String, RubricScoreData> rubricsScores;

    /**
     * DTO for individual rubric scores.
     */
    @Data
    @Builder
    public static class RubricScoreData {
        /**
         * Score achieved for this rubric.
         */
        private Integer score;

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
