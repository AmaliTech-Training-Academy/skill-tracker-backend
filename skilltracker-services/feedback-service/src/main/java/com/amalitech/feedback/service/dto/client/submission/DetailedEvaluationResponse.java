package com.amalitech.feedback.service.dto.client.submission;

import lombok.Data;

import java.util.List;

/**
 * Comprehensive Data Transfer Object (DTO) used to structure the detailed, multi-dimensional
 * evaluation response from the AI Evaluation Service back to the Task Service.
 * <p>
 * This complex DTO serves as the final, rich evaluation artifact, breaking down the grading
 * into correctness, efficiency, style, and an overall summary, which is then persisted or
 * shown to the user. Its structure matches the sophisticated output schema defined for the
 * specialized AI grading agents.
 */
@Data
public class DetailedEvaluationResponse {
    private Evaluation evaluation;

    /**
     * The root container for all specialized evaluation aspects of the submission.
     */
    @Data
    public static class Evaluation {
        private CorrectnessEvaluation correctness;
        private EfficiencyEvaluation efficiency;
        private StyleEvaluation style;
        private OverallEvaluation overall;
    }

    /**
     * Evaluation section dedicated to measuring the functional correctness and adherence
     * to requirements, typically via test case results.
     */
    @Data
    public static class CorrectnessEvaluation {
        private double score;
        private double maxScore;
        private int percentage;
        private String feedback;
        private List<TestResult> testResults;
        private List<CriterionResult> criteriaMet;
    }

    /**
     * Evaluation section dedicated to measuring the algorithmic efficiency and resource
     * utilization (time and space complexity) of the user's solution.
     */
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

    /**
     * Evaluation section dedicated to measuring code quality, readability, adherence to
     * style guides, and general programming best practices.
     */
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

    /**
     * Final section providing a synthesized summary of the entire evaluation, including
     * total score, grade, pass/fail status, and key takeaways for the user.
     */
    @Data
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
     * Represents the detailed outcome of a single unit test or example case run against
     * the user's submitted code.
     */
    @Data
    public static class TestResult {
        private String testCase;
        private boolean passed;
        private String input;
        private String expectedOutput;
        private String actualOutput;
        private String feedback;
    }

    /**
     * Represents the assessment result for a specific, high-level criterion or requirement
     * within any evaluation category (Correctness, Efficiency, Style).
     */
    @Data
    public static class CriterionResult {
        private String criterion;
        private boolean met;
        private String feedback;
    }
}