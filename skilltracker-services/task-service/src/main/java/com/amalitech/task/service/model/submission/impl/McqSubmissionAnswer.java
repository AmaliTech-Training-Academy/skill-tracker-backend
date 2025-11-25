package com.amalitech.task.service.model.submission.impl;

import com.amalitech.task.service.model.submission.SubmissionAnswer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Concrete implementation of the {@link SubmissionAnswer} interface, specifically designed
 * to capture user answers for a Multiple-Choice Question (MCQ) task.
 * <p>
 * This class serves as the clean data structure for receiving and validating submission
 * payloads via REST APIs, supporting multiple questions in a single submission.
 * It enforces that selected option indices are non-negative integers.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class McqSubmissionAnswer implements SubmissionAnswer {
    private List<QuestionAnswer> answers;

    /**
     * Represents a single answer to one question in an MCQ task.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionAnswer {
        private String questionNumber;
        private int selectedOption;
    }
}