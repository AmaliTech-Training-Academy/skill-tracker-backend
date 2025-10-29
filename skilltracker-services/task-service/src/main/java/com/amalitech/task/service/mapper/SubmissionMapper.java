package com.amalitech.task.service.mapper;

import com.amalitech.common.event.events.SubmissionCreatedEvent;
import com.amalitech.task.service.dto.TaskSubmissionDTO;
import com.amalitech.task.service.model.Task;
import com.amalitech.task.service.model.TaskSubmission;
import com.amalitech.task.service.model.content.impl.CodingTaskContent;
import com.amalitech.task.service.model.submission.impl.CodingSubmissionAnswer;
import com.amalitech.task.service.model.submission.impl.EssaySubmissionAnswer;

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
public class SubmissionMapper {

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
                        .taskId(submission.getTask().getId());

        if (submission.getAnswer() instanceof CodingSubmissionAnswer answer &&
                submission.getTask().getContent() instanceof CodingTaskContent content) {

            builder.taskType("CODING");
            builder.codeToEvaluate(answer.getCode());
            builder.languageId(answer.getLanguageId());

            List<SubmissionCreatedEvent.TestCaseData> testCaseData = content.getExamples().stream()
                    .map(ex -> SubmissionCreatedEvent.TestCaseData.builder()
                            .input(ex.getInput())
                            .expectedOutput(ex.getOutput())
                            .build())
                    .toList();
            builder.testCases(testCaseData);
        }
        else if (submission.getAnswer() instanceof EssaySubmissionAnswer answer) {
            builder.taskType("ESSAY");
            builder.essayToEvaluate(answer.getSubmissionText());
        }

        return builder.build();
    }
}