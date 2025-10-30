package com.amalitech.task.service.model.feedback.impl;

import com.amalitech.task.service.model.feedback.SubmissionFeedback;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Concrete implementation of {@link SubmissionFeedback} designed to store the comprehensive,
 * multi-dimensional evaluation results and suggestions for an Essay task submission.
 * <p>
 * This structure captures qualitative scoring metrics and actionable suggestions generated
 * by sophisticated Natural Language Processing (NLP) AI agents, enabling detailed and
 * adaptive feedback for the user's written communication skills.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EssaySubmissionFeedback implements SubmissionFeedback {
    private double grammarScore;
    private double relevanceScore;
    private String toneAnalysis;
    private List<FeedbackSuggestion> suggestions;

    /**
     * Represents a specific, actionable suggestion for improvement within the submitted essay text.
     * <p>
     * This nested class allows the feedback mechanism to pinpoint areas in the user's writing
     * and provide a concrete path for correction or enhancement.
     */
    @Data
    public static class FeedbackSuggestion {
        private String originalText;
        private String suggestedChange;
        private String comment;
    }
}