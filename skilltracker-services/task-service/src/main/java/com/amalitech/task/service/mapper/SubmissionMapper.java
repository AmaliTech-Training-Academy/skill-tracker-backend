package com.amalitech.task.service.mapper;

import com.amalitech.common.event.events.SubmissionCreatedEvent;
import com.amalitech.task.service.dto.TaskSubmissionDTO;
import com.amalitech.task.service.model.TaskSubmission;
import com.amalitech.task.service.model.content.impl.CodingTaskContent;
import com.amalitech.task.service.model.submission.impl.CodingSubmissionAnswer;
import com.amalitech.task.service.model.submission.impl.EssaySubmissionAnswer;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SubmissionMapper {

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
     * Maps the internal entity to the public SubmissionCreatedEvent.
     * This now includes all data needed by the feedback-service.
     */
    public SubmissionCreatedEvent toCreatedEvent(TaskSubmission submission) {

        SubmissionCreatedEvent.SubmissionCreatedEventBuilder builder =
                SubmissionCreatedEvent.builder()
                        .submissionId(submission.getId())
                        .userId(submission.getUserId())
                        .taskId(submission.getTask().getId());

        // --- Handle CODING ---
        if (submission.getAnswer() instanceof CodingSubmissionAnswer answer &&
                submission.getTask().getContent() instanceof CodingTaskContent content) {

            builder.taskType("CODING");
            builder.codeToEvaluate(answer.getCode());
            builder.languageId(answer.getLanguageId());

            // --- Extract and carry the test cases ---
            List<SubmissionCreatedEvent.TestCaseData> testCaseData = content.getExamples().stream()
                    .map(ex -> SubmissionCreatedEvent.TestCaseData.builder()
                            .input(ex.getInput())
                            .expectedOutput(ex.getOutput())
                            .build())
                    .toList();
            builder.testCases(testCaseData);
        }
        // --- Handle ESSAY ---
        else if (submission.getAnswer() instanceof EssaySubmissionAnswer answer) {
            builder.taskType("ESSAY");
            builder.essayToEvaluate(answer.getSubmissionText());
        }

        return builder.build();
    }
}