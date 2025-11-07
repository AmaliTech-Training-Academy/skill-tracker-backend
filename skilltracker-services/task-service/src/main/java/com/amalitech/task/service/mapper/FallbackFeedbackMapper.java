package com.amalitech.task.service.mapper;

import com.amalitech.common.event.events.SubmissionEvaluatedEvent;
import com.amalitech.task.service.model.feedback.impl.CodingSubmissionFeedback;
import com.amalitech.task.service.model.feedback.impl.EssaySubmissionFeedback;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A dedicated mapper for creating "fallback" feedback models.
 * This class is used when detailed JSON feedback from an event
 * is missing or fails to parse, ensuring the user still receives
 * a basic (but correctly structured) feedback response.
 */
@Component
public class FallbackFeedbackMapper {

    /**
     * Maps the flat data from a SubmissionEvaluatedEvent into the
     * NEW nested CodingSubmissionFeedback structure.
     */
    public CodingSubmissionFeedback createBasicCodingFeedback(SubmissionEvaluatedEvent event) {

        List<CodingSubmissionFeedback.TestResult> testResults = List.of();
        if (event.getTestResults() != null) {
            testResults = event.getTestResults().stream()
                    .map(tr -> CodingSubmissionFeedback.TestResult.builder()
                            .testCase(tr.getInput())
                            .passed(tr.isPassed())
                            .input(tr.getInput())
                            .expectedOutput(tr.getExpectedOutput())
                            .actualOutput(tr.getActualOutput())
                            .feedback(tr.getStatusDescription())
                            .build())
                    .collect(Collectors.toList());
        }

        var correctness = CodingSubmissionFeedback.CorrectnessEvaluation.builder()
                .feedback("See test results for correctness.")
                .testResults(testResults)
                .percentage(event.getScore())
                .score(event.getScore())
                .maxScore(100)
                .build();

        var overall = CodingSubmissionFeedback.OverallEvaluation.builder()
                .summary(event.getOverallFeedback())
                .percentage(event.getScore())
                .totalScore(event.getScore())
                .maxScore(100)
                .passed(event.isCorrect())
                .xpEarned(0)
                .keyStrengths(List.of())
                .keyImprovements(List.of())
                .build();

        var evaluation = CodingSubmissionFeedback.Evaluation.builder()
                .correctness(correctness)
                .overall(overall)
                .efficiency(CodingSubmissionFeedback.EfficiencyEvaluation.builder()
                        .feedback("N/A")
                        .percentage(0)
                        .score(0)
                        .build())
                .style(CodingSubmissionFeedback.StyleEvaluation.builder()
                        .feedback("N/A")
                        .percentage(0)
                        .score(0)
                        .build())
                .build();

        return CodingSubmissionFeedback.builder()
                .evaluation(evaluation)
                .build();
    }

    /**
     * Creates basic essay feedback when detailed feedback is not available.
     * (This is your original, unchanged helper method)
     */
    public EssaySubmissionFeedback createBasicEssayFeedback(SubmissionEvaluatedEvent event) {
        EssaySubmissionFeedback essayFeedback = new EssaySubmissionFeedback();

        essayFeedback.setGrammarScore(0.0);
        essayFeedback.setRelevanceScore(event.getScore() / 100.0);
        essayFeedback.setToneAnalysis("Essay evaluation completed");
        essayFeedback.setSuggestions(List.of());

        return essayFeedback;
    }
}