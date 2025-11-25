package com.amalitech.feedback.service.dto.client.submission.impl;

import com.amalitech.feedback.service.dto.client.submission.SubmissionAnswer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Concrete implementation of the {@link SubmissionAnswer} interface for MCQ task submissions.
 * Captures the user's answers for multiple questions in a single submission.
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
        private String questionNumber;  // Question identifier (e.g., "1", "2", "3")
        private int selectedOption;     // Index of the selected option (0-based)
    }
}
