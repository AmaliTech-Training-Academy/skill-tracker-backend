package com.amalitech.task.service.mapper;

import com.amalitech.common.event.events.SubmissionCreatedEvent;
import com.amalitech.task.service.dto.TaskSubmissionDTO;
import com.amalitech.task.service.model.Task;
import com.amalitech.task.service.model.TaskSubmission;
import com.amalitech.task.service.model.content.impl.CodingTaskContent;
import com.amalitech.task.service.model.content.impl.McqTaskContent;
import com.amalitech.task.service.model.submission.impl.CodingSubmissionAnswer;
import com.amalitech.task.service.model.submission.impl.EssaySubmissionAnswer;
import com.amalitech.task.service.model.submission.impl.McqSubmissionAnswer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper component responsible for converting between the internal persistence entity
 * {@link TaskSubmission}, the external Data Transfer Object (DTO) {@link TaskSubmissionDTO},
 * and the asynchronous event object {@link SubmissionCreatedEvent}.
 * <p>
 * This class ensures that data is correctly shaped and encapsulated before being sent
 * over the wire (API response) or pushed to a message broker (Event Bus).
 */
@Component
@Slf4j
public class SubmissionMapper {

    private final ObjectMapper objectMapper;

    public SubmissionMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Converts the internal persistence entity {@link TaskSubmission} into the public
     * REST response DTO {@link TaskSubmissionDTO}.
     * <p>
     * This method handles null checks and extracts only the relevant fields necessary
     * for client consumption, including details from the associated {@link Task}.
     *
     * @param submission The internal {@link TaskSubmission} entity fetched from the database.
     * @return The public-facing {@link TaskSubmissionDTO}, or {@code null} if the input submission is null.
     */
    public TaskSubmissionDTO toDTO(TaskSubmission submission) {
        if (submission == null) {
            return null;
        }

        TaskSubmissionDTO.TaskSubmissionDTOBuilder builder = TaskSubmissionDTO.builder()
                .id(submission.getId())
                .userId(submission.getUserId())
                .status(submission.getStatus())
                .answer(submission.getAnswer())
                .feedback(submission.getFeedback())
                .isCorrect(submission.getIsCorrect())
                .scoreEarned(submission.getScoreEarned())
                .submittedAt(submission.getSubmittedAt())
                .evaluatedAt(submission.getEvaluatedAt());

        if (submission.getTask() != null) {
            builder.taskId(submission.getTask().getId());
        }

        return builder.build();
    }


    /**
     * Maps the internal {@link TaskSubmission} entity into the asynchronous event
     * {@link SubmissionCreatedEvent} for consumption by the evaluation service.
     * <p>
     * This method contains polymorphic logic to correctly extract content based on
     * the submission type (e.g., code, essay) and package it with the necessary
     * metadata (like test cases for coding submissions) required by the downstream
     * AI evaluation agents.
     *
     * @param submission The fully populated {@link TaskSubmission} entity.
     * @return A {@link SubmissionCreatedEvent} ready to be published to the message broker.
     */
    public SubmissionCreatedEvent toCreatedEvent(TaskSubmission submission) {

        SubmissionCreatedEvent.SubmissionCreatedEventBuilder builder =
                SubmissionCreatedEvent.builder()
                        .submissionId(submission.getId())
                        .userId(submission.getUserId())
                        .taskId(submission.getTask().getId())
                        .taskType(submission.getTask().getType().name());

        if (submission.getAnswer() instanceof CodingSubmissionAnswer answer &&
                submission.getTask().getContent() instanceof CodingTaskContent content) {
            builder.contentToEvaluate(answer.getCode());
            builder.languageId(answer.getLanguageId());
            builder.submissionHarness(content.getSubmissionHarness());

            List<SubmissionCreatedEvent.TestCaseData> testCaseData = content.getExamples().stream()
                    .map(ex -> SubmissionCreatedEvent.TestCaseData.builder()
                            .input(ex.getInput())
                            .expectedOutput(ex.getOutput())
                            .build())
                    .toList();

            builder.testCases(testCaseData);

            populateCommonTaskFields(builder, submission.getTask());
        }
        else if (submission.getAnswer() instanceof EssaySubmissionAnswer answer) {
            builder.contentToEvaluate(answer.getSubmissionText());

            populateCommonTaskFields(builder, submission.getTask());


            if (submission.getTask() != null &&
                    submission.getTask().getContent() instanceof com.amalitech.task.service.model.content.impl.EssayTaskContent essayContent) {
                builder.detailedInstructions(essayContent.getDetailedInstructions());

                try {
                    String evaluationCriteria = objectMapper.writeValueAsString(essayContent.getEvaluationCriteria());
                    String rubric = objectMapper.writeValueAsString(essayContent.getRubric());

                    builder.evaluationCriteria(evaluationCriteria);
                    builder.rubric(rubric);
                } catch (Exception e) {
                    log.warn("Failed to serialize essay evaluation criteria/rubric for task {}: {}",
                            submission.getTask().getId(), e.getMessage());
                }
            }
        }
        else if (submission.getAnswer() instanceof McqSubmissionAnswer answer) {
            // For MCQ tasks, send the answer as JSON
            try {
                String answerJson = objectMapper.writeValueAsString(answer);
                builder.contentToEvaluate(answerJson);
            } catch (Exception e) {
                log.error("Failed to serialize MCQ answer for submission {}: {}", submission.getId(), e.getMessage());
                throw new RuntimeException("Failed to serialize MCQ answer", e);
            }

            // Also send the task content (questions, options, correct answers) for evaluation
            if (submission.getTask() != null &&
                    submission.getTask().getContent() instanceof McqTaskContent mcqContent) {
                try {
                    String contentJson = objectMapper.writeValueAsString(mcqContent);
                    // Use taskDescription field to pass MCQ content to evaluator
                    builder.taskDescription(contentJson);
                } catch (Exception e) {
                    log.error("Failed to serialize MCQ content for task {}: {}", submission.getTask().getId(), e.getMessage());
                    throw new RuntimeException("Failed to serialize MCQ content", e);
                }
            }

            populateCommonTaskFields(builder, submission.getTask());
        }

        return builder.build();
    }

    /**
     * Populates common task fields that are needed for AI evaluation context.
     * These fields are populated for all task types.
     *
     * @param builder the event builder
     * @param task the task entity
     */
    private void populateCommonTaskFields(SubmissionCreatedEvent.SubmissionCreatedEventBuilder builder, Task task) {
        if (task != null) {
            builder.skillName(task.getTaskDefinition() != null && task.getTaskDefinition().getSkill() != null ?
                    task.getTaskDefinition().getSkill().getName() : null);
            builder.difficulty(task.getDifficulty() != null ? task.getDifficulty().name() : null);
            builder.taskTitle(task.getTitle());
            builder.taskDescription(task.getDescription());
        }
    }
}