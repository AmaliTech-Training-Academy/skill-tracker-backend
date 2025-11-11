package com.amalitech.task.service.mapper;

import com.amalitech.common.event.events.SubmissionEvaluatedEvent;
import com.amalitech.common.event.events.TaskCompletedEvent;
import com.amalitech.task.service.model.Task;
import com.amalitech.task.service.model.TaskSubmission;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Mapper component responsible for converting task submission evaluation results
 * into {@link TaskCompletedEvent} for consumption by analytics services.
 * <p>
 * This mapper combines data from:
 * <ul>
 * <li>Task entity (description, type, difficulty)</li>
 * <li>Submission entity (userId, taskId, skillId)</li>
 * <li>SubmissionEvaluatedEvent (score, pass/fail status, feedback)</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TaskCompletionMapper {

    private final ObjectMapper objectMapper;

    /**
     * Converts task submission evaluation data into a TaskCompletedEvent.
     * <p>
     * This method combines the task definition, submission record, and evaluation
     * results into a comprehensive event that includes all necessary analytics data.
     *
     * @param submission The {@link TaskSubmission} containing user and task info.
     * @param task The {@link Task} entity with task details.
     * @param evaluatedEvent The {@link SubmissionEvaluatedEvent} with evaluation results.
     * @param skillId The skill ID associated with this task.
     * @param totalXpEarned The total XP earned by the user.
     * @return A {@link TaskCompletedEvent} ready for publishing to analytics.
     */
    public TaskCompletedEvent toTaskCompletedEvent(
            TaskSubmission submission,
            Task task,
            SubmissionEvaluatedEvent evaluatedEvent,
            java.util.UUID skillId,
            Integer totalXpEarned) {

        log.debug("Mapping task completion event for submission: {} and task: {}",
                submission.getId(), task.getId());

        return TaskCompletedEvent.builder()
                .userId(submission.getUserId())
                .skillId(skillId)
                .taskId(task.getId().toString())
                .taskDescription(task.getDescription())
                .totalXpEarned(totalXpEarned)
                .passed(evaluatedEvent.isCorrect())
                .completedAt(Instant.now())
                .taskType(task.getType().toString())
                .taskDifficulty(task.getDifficulty().toString())
                .rubricsScores(extractRubricScores(evaluatedEvent))
                .build();
    }

    /**
     * Extracts rubric scores from the detailed feedback JSON in the evaluation event.
     * <p>
     * Parses the detailedFeedback JSON and extracts category scores. Supports:
     * <ul>
     * <li>Coding tasks: correctness, efficiency, style</li>
     * <li>Written tasks: completeness, accuracy, clarity, depth</li>
     * </ul>
     *
     * @param evaluatedEvent The evaluation event containing detailed feedback.
     * @return A map of rubric names to their scores (score, maxScore, percentage).
     */
    private Map<String, TaskCompletedEvent.RubricScoreData> extractRubricScores(SubmissionEvaluatedEvent evaluatedEvent) {
        Map<String, TaskCompletedEvent.RubricScoreData> rubricScores = new HashMap<>();

        if (evaluatedEvent.getDetailedFeedback() == null || evaluatedEvent.getDetailedFeedback().trim().isEmpty()) {
            log.debug("No detailed feedback available for rubric score extraction");
            return rubricScores;
        }

        try {
            JsonNode root = objectMapper.readTree(evaluatedEvent.getDetailedFeedback());
            JsonNode evaluation = root.get("evaluation");

            if (evaluation == null) {
                log.debug("No evaluation node found in detailed feedback");
                return rubricScores;
            }

            // Extract coding rubrics (correctness, efficiency, style)
            extractCodingRubrics(evaluation, rubricScores);

            // Extract written rubrics (completeness, accuracy, clarity, depth)
            extractWrittenRubrics(evaluation, rubricScores);

        } catch (Exception e) {
            log.warn("Failed to parse rubric scores from detailed feedback: {}", e.getMessage());
        }

        return rubricScores;
    }

    /**
     * Extracts coding task rubric scores (correctness, efficiency, style).
     *
     * @param evaluation The evaluation node from the feedback JSON.
     * @param rubricScores The map to populate with extracted scores.
     */
    private void extractCodingRubrics(JsonNode evaluation, Map<String, TaskCompletedEvent.RubricScoreData> rubricScores) {
        String[] codingRubrics = {"correctness", "efficiency", "style"};

        for (String rubric : codingRubrics) {
            JsonNode rubricNode = evaluation.get(rubric);
            if (rubricNode != null) {
                TaskCompletedEvent.RubricScoreData scoreData = extractScoreData(rubricNode, rubric);
                if (scoreData != null) {
                    rubricScores.put(rubric, scoreData);
                    log.debug("Extracted {} rubric score: {}", rubric, scoreData.getPercentage());
                }
            }
        }
    }

    /**
     * Extracts written task rubric scores (completeness, accuracy, clarity, depth).
     *
     * @param evaluation The evaluation node from the feedback JSON.
     * @param rubricScores The map to populate with extracted scores.
     */
    private void extractWrittenRubrics(JsonNode evaluation, Map<String, TaskCompletedEvent.RubricScoreData> rubricScores) {
        String[] writtenRubrics = {"completeness", "accuracy", "clarity", "depth"};

        for (String rubric : writtenRubrics) {
            JsonNode rubricNode = evaluation.get(rubric);
            if (rubricNode != null) {
                TaskCompletedEvent.RubricScoreData scoreData = extractScoreData(rubricNode, rubric);
                if (scoreData != null) {
                    rubricScores.put(rubric, scoreData);
                    log.debug("Extracted {} rubric score: {}", rubric, scoreData.getPercentage());
                }
            }
        }
    }

    /**
     * Extracts score, percentage, and calculates maxScore from a rubric node.
     * <p>
     * The maxScore is calculated based on the rubric name and its weight in the evaluation:
     * <ul>
     * <li>Coding - Correctness: 50</li>
     * <li>Coding - Efficiency: 30</li>
     * <li>Coding - Style: 20</li>
     * <li>Written - Completeness: 25</li>
     * <li>Written - Accuracy: 30</li>
     * <li>Written - Clarity: 25</li>
     * <li>Written - Depth: 20</li>
     * </ul>
     *
     * @param rubricNode The rubric node containing score and percentage.
     * @param rubricName The name of the rubric category.
     * @return A RubricScoreData with score, maxScore, and percentage, or null if extraction fails.
     */
    private TaskCompletedEvent.RubricScoreData extractScoreData(JsonNode rubricNode, String rubricName) {
        try {
            double score = rubricNode.get("score").asDouble(0.0);
            int percentage = rubricNode.get("percentage").asInt(0);

            // Calculate maxScore based on the rubric name and weight
            int maxScore = getMaxScoreForRubric(rubricName);

            return TaskCompletedEvent.RubricScoreData.builder()
                    .score((int) score)
                    .maxScore(maxScore)
                    .percentage(percentage)
                    .build();

        } catch (Exception e) {
            log.warn("Failed to extract score data from rubric node '{}': {}", rubricName, e.getMessage());
            return null;
        }
    }

    /**
     * Determines the maximum score for a rubric category based on its weight.
     *
     * @param rubricName The name of the rubric category.
     * @return The maximum score for this rubric category.
     */
    private int getMaxScoreForRubric(String rubricName) {
        return switch (rubricName) {
            // Coding task rubrics
            case "correctness" -> 50;
            case "efficiency" -> 30;
            case "style" -> 20;
            // Written task rubrics
            case "completeness" -> 25;
            case "accuracy" -> 30;
            case "clarity" -> 25;
            case "depth" -> 20;
            default -> 100; // Fallback for unknown rubrics
        };
    }
}
