package com.amalitech.task.service.model.feedback.impl;

import com.amalitech.task.service.model.feedback.SubmissionFeedback;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Concrete implementation of {@link SubmissionFeedback} for CODING tasks.
 * This structure now mirrors the rich 'DetailedEvaluationResponse' from
 * the feedback-service, allowing for the storage of complex,
 * multi-dimensional AI feedback.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CodingSubmissionFeedback implements SubmissionFeedback {

    private Evaluation evaluation;

    /**
     * The root container for all specialized evaluation aspects.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Evaluation {
        private CorrectnessEvaluation correctness;
        private EfficiencyEvaluation efficiency;
        private StyleEvaluation style;
        private OverallEvaluation overall;
    }

    /**
     * Evaluation section for functional correctness.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CorrectnessEvaluation {
        private double score;
        private double maxScore;
        private int percentage;
        private String feedback;
        private List<TestResult> testResults;
        private List<CriterionResult> criteriaMet;
    }

    /**
     * Evaluation section for algorithmic efficiency.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EfficiencyEvaluation {
        private double score;
        private double maxScore;
        private int percentage;
        private String feedback;
        private String timeComplexity;
        private String spaceComplexity;
        private String analysis;
        private List<CriterionResult> criteriaMet;
    }

    /**
     * Evaluation section for code quality and style.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StyleEvaluation {
        private double score;
        private double maxScore;
        private int percentage;
        private String feedback;
        private List<String> strengths;
        private List<String> improvements;
        private List<CriterionResult> criteriaMet;
    }

    /**
     * Final summary section.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OverallEvaluation {
        private double totalScore;
        private double maxScore;
        private int percentage;
        private int xpEarned;
        private boolean passed;
        private String summary;
        private List<String> keyStrengths;
        private List<String> keyImprovements;
    }

    /**
     * Represents the outcome of a single test case.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TestResult {
        private String testCase;
        private boolean passed;
        private String input;
        private String expectedOutput;
        private String actualOutput;
        private String feedback;
    }

    /**
     * Represents the assessment of a specific criterion.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CriterionResult {
        private String criterion;
        private boolean met;
        private String feedback;
    }
}