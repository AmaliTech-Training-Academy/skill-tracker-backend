package com.amalitech.task.service.mapper;

import com.amalitech.common.event.events.SubmissionEvaluatedEvent;
import com.amalitech.common.event.events.TaskCompletedEvent;
import com.amalitech.task.service.model.Task;
import com.amalitech.task.service.model.TaskSubmission;
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
@Slf4j
public class TaskCompletionMapper {

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
                .rubricsScores(extractRubricScores())
                .build();
    }

    /**
     * Extracts rubric scores from evaluation data.
     * <p>
     * Currently returns an empty map as rubric scores are not yet available
     * in the SubmissionEvaluatedEvent. This method is a placeholder for future
     * enhancement when rubric scoring is implemented.
     *
     * @return A map of rubric scores (empty until rubric scoring is implemented).
     */
    private Map<String, TaskCompletedEvent.RubricScoreData> extractRubricScores() {
        // TODO: Extract rubric scores from evaluation data when available
        return new HashMap<>();
    }
}
