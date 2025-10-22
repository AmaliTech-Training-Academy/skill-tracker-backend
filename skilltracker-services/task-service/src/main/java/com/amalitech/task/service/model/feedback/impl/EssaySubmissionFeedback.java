package com.amalitech.task.service.model.feedback.impl;

import com.amalitech.task.service.model.feedback.SubmissionFeedback;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EssaySubmissionFeedback implements SubmissionFeedback {
    private double grammarScore;
    private double relevanceScore;
    private String toneAnalysis;
    private List<FeedbackSuggestion> suggestions;

    @Data
    public static class FeedbackSuggestion {
        private String originalText;
        private String suggestedChange;
        private String comment;
    }
}
