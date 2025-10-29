package com.amalitech.feedback.service.dto.client.submission;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Comprehensive evaluation response matching the detailed prompt structure.
 */
@Data
public class DetailedEvaluationResponse {
    private Evaluation evaluation;

    @Data
    public static class Evaluation {
        private CorrectnessEvaluation correctness;
        private EfficiencyEvaluation efficiency;
        private StyleEvaluation style;
        private OverallEvaluation overall;
    }

    @Data
    public static class CorrectnessEvaluation {
        private double score;
        private double maxScore;
        private int percentage;
        private String feedback;
        private List<TestResult> testResults;
        private List<CriterionResult> criteriaMet;
    }

    @Data
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

    @Data
    public static class StyleEvaluation {
        private double score;
        private double maxScore;
        private int percentage;
        private String feedback;
        private List<String> strengths;
        private List<String> improvements;
        private List<CriterionResult> criteriaMet;
    }

    @Data
    public static class OverallEvaluation {
        private double totalScore;
        private double maxScore;
        private int percentage;
        private int xpEarned;
        private boolean passed;
        private String grade;
        private String summary;
        private List<String> keyStrengths;
        private List<String> keyImprovements;
    }

    @Data
    public static class TestResult {
        private String testCase;
        private boolean passed;
        private String input;
        private String expectedOutput;
        private String actualOutput;
        private String feedback;
    }

    @Data
    public static class CriterionResult {
        private String criterion;
        private boolean met;
        private String feedback;
    }
}
