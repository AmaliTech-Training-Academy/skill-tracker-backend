package com.amalitech.feedback.service.dto.client.submission.impl;

import com.amalitech.feedback.service.dto.client.submission.SubmissionFeedback;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Concrete implementation of {@link SubmissionFeedback} for MCQ task submissions.
 * Stores evaluation results and per-question feedback for multiple-choice assessments.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class McqSubmissionFeedback implements SubmissionFeedback {
    private List<QuestionFeedback> feedbacks;
    private int totalCorrect;
    private int totalQuestions;
    private double scorePercentage;

    /**
     * Feedback for a single question's evaluation.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionFeedback {
        private String questionNumber;      // Question identifier
        private boolean isCorrect;          // Whether the answer was correct
        private int correctOption;          // Index of the correct option
        private String explanation;         // Explanation of the correct answer
    }
}
