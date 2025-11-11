package com.amalitech.task.service.validation;

import com.amalitech.common.event.events.TaskCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Validates TaskCompletedEvent data integrity and consistency.
 *
 * <p>This validator ensures that analytics events contain valid and reasonable data
 * before publication. It checks for required fields, data ranges, and consistency
 * across related fields.</p>
 */
@Slf4j
@Component
public class TaskCompletedEventValidator {

    private static final int MIN_XP = 0;
    private static final int MAX_XP = 10000;
    private static final int MIN_RUBRIC_SCORE = 0;
    private static final int MAX_RUBRIC_SCORE = 100;

    /**
     * Validates a TaskCompletedEvent and returns any validation errors.
     *
     * @param event the event to validate
     * @return list of validation error messages (empty if valid)
     */
    public List<String> validate(TaskCompletedEvent event) {
        List<String> errors = new ArrayList<>();

        if (event == null) {
            errors.add("TaskCompletedEvent is null");
            return errors;
        }

        // Validate required fields
        if (event.getUserId() == null) {
            errors.add("userId is required");
        }
        if (event.getTaskId() == null) {
            errors.add("taskId is required");
        }
        if (event.getSkillId() == null) {
            errors.add("skillId is required");
        }
        if (event.getTaskType() == null || event.getTaskType().isBlank()) {
            errors.add("taskType is required");
        }
        if (event.getCompletedAt() == null) {
            errors.add("completedAt is required");
        }

        // Validate XP earned
        if (event.getTotalXpEarned() != null) {
            if (event.getTotalXpEarned() < MIN_XP || event.getTotalXpEarned() > MAX_XP) {
                errors.add(String.format(
                    "totalXpEarned must be between %d and %d, got %d",
                    MIN_XP, MAX_XP, event.getTotalXpEarned()
                ));
            }
        }

        // Validate passed status consistency
        if (event.getPassed() != null) {
            // If passed, XP should be earned (unless it's a zero-XP task)
            if (event.getPassed() && event.getTotalXpEarned() != null && event.getTotalXpEarned() < 0) {
                errors.add("Cannot have passed=true with negative totalXpEarned");
            }
        }

        // Validate rubric scores
        if (event.getRubricsScores() != null && !event.getRubricsScores().isEmpty()) {
            errors.addAll(validateRubricScores(event.getRubricsScores(), event.getTaskType()));
        }

        if (!errors.isEmpty()) {
            log.warn("TaskCompletedEvent validation failed for user {} and task {}: {}",
                event.getUserId(), event.getTaskId(), String.join("; ", errors));
        }

        return errors;
    }

    /**
     * Validates individual rubric scores.
     *
     * @param rubricsScores the rubric scores map
     * @param taskType the task type to determine expected rubrics
     * @return list of validation errors for rubrics
     */
    private List<String> validateRubricScores(
            Map<String, TaskCompletedEvent.RubricScoreData> rubricsScores,
            String taskType) {
        List<String> errors = new ArrayList<>();

        for (Map.Entry<String, TaskCompletedEvent.RubricScoreData> entry : rubricsScores.entrySet()) {
            String rubricName = entry.getKey();
            TaskCompletedEvent.RubricScoreData scoreData = entry.getValue();

            if (scoreData == null) {
                errors.add(String.format("Rubric '%s' has null score data", rubricName));
                continue;
            }

            // Validate score is not negative
            if (scoreData.getScore() != null && scoreData.getScore() < 0) {
                errors.add(String.format("Rubric '%s' score is negative: %f", rubricName, scoreData.getScore()));
            }

            // Validate maxScore is positive
            if (scoreData.getMaxScore() == null || scoreData.getMaxScore() <= 0) {
                errors.add(String.format("Rubric '%s' maxScore must be positive, got %d", 
                    rubricName, scoreData.getMaxScore()));
            }

            // Validate score doesn't exceed maxScore
            if (scoreData.getScore() != null && scoreData.getMaxScore() != null &&
                scoreData.getScore() > scoreData.getMaxScore()) {
                errors.add(String.format(
                    "Rubric '%s' score exceeds maxScore: %f > %d",
                    rubricName, scoreData.getScore(), scoreData.getMaxScore()
                ));
            }

            // Validate percentage is within valid range
            if (scoreData.getPercentage() != null &&
                (scoreData.getPercentage() < 0 || scoreData.getPercentage() > 100)) {
                errors.add(String.format(
                    "Rubric '%s' percentage must be 0-100, got %d",
                    rubricName, scoreData.getPercentage()
                ));
            }
        }

        // Validate expected rubrics for task type
        errors.addAll(validateExpectedRubrics(rubricsScores.keySet(), taskType));

        return errors;
    }

    /**
     * Validates that expected rubrics are present for the given task type.
     *
     * @param presentRubrics the rubric names found in the event
     * @param taskType the task type
     * @return list of validation errors for missing/unexpected rubrics
     */
    private List<String> validateExpectedRubrics(java.util.Set<String> presentRubrics, String taskType) {
        List<String> errors = new ArrayList<>();

        if (taskType == null) {
            return errors; // Can't validate without taskType
        }

        switch (taskType.toUpperCase()) {
            case "CODING":
                // Coding tasks should have correctness, efficiency, style
                validateRubricsPresent(presentRubrics, errors, "correctness", "efficiency", "style");
                break;
            case "ESSAY":
            case "WRITTEN":
                // Essay tasks should have completeness, accuracy, clarity, depth
                validateRubricsPresent(presentRubrics, errors, "completeness", "accuracy", "clarity", "depth");
                break;
            case "MCQ":
                // MCQ tasks may not have detailed rubrics, which is acceptable
                break;
            default:
                log.debug("No rubric validation defined for task type: {}", taskType);
        }

        return errors;
    }

    /**
     * Helper to validate that expected rubrics are present.
     *
     * @param presentRubrics the rubrics found in the event
     * @param errors the error list to append to
     * @param expectedRubrics the rubrics that should be present
     */
    private void validateRubricsPresent(java.util.Set<String> presentRubrics,
                                       List<String> errors,
                                       String... expectedRubrics) {
        for (String expected : expectedRubrics) {
            if (!presentRubrics.contains(expected)) {
                errors.add(String.format("Expected rubric '%s' is missing", expected));
            }
        }
    }

    /**
     * Checks if an event has any validation errors.
     *
     * @param event the event to validate
     * @return true if the event is valid, false otherwise
     */
    public boolean isValid(TaskCompletedEvent event) {
        return validate(event).isEmpty();
    }
}
