package com.amalitech.feedback.service.dto.client.submission.impl;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Feedback structure for essay task evaluations.
 * Contains detailed assessment across completeness, accuracy, clarity, and depth dimensions.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EssaySubmissionFeedback {

    private Evaluation evaluation;

    /**
     * The main evaluation container matching the AI prompt structure.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Evaluation {
        private CategoryEvaluation completeness;
        private CategoryEvaluation accuracy;
        private CategoryEvaluation clarity;
        private CategoryEvaluation depth;
        private OverallEvaluation overall;
    }

    /**
     * Evaluation details for a single assessment category.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoryEvaluation {
        private Double score;
        private Integer percentage;
        private String feedback;
        private List<CriteriaMet> criteriaMet;
        private List<String> strengths;
        private List<String> improvements;
    }

    /**
     * Assessment of individual evaluation criteria.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CriteriaMet {
        private String criterion;
        private Boolean met;
        private String feedback;
    }

    /**
     * Overall evaluation summary and scoring.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OverallEvaluation {
        private Double totalScore;
        private Double percentage;
        private Double xpEarned;
        private Double maxXP;
        private Boolean passed;
        private String summary;
        private List<String> keyStrengths;
        private List<String> keyImprovements;
        private String nextSteps;
    }
}
