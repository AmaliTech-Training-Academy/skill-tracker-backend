package com.amalitech.task.service.model.feedback.impl;

import com.amalitech.task.service.model.feedback.SubmissionFeedback;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Concrete implementation of {@link SubmissionFeedback} specifically designed to store
 * the evaluation results and feedback for a Multiple-Choice Question (MCQ) task submission.
 * <p>
 * This feedback structure provides per-question feedback for multiple-choice assessments,
 * including overall statistics and detailed explanations for each question.
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