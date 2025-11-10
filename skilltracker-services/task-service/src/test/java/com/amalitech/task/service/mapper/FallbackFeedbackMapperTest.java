package com.amalitech.task.service.mapper;

import com.amalitech.common.event.events.SubmissionEvaluatedEvent;
import com.amalitech.task.service.model.feedback.impl.CodingSubmissionFeedback;
import com.amalitech.task.service.model.feedback.impl.EssaySubmissionFeedback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FallbackFeedbackMapperTest {

    private FallbackFeedbackMapper fallbackFeedbackMapper;
    private UUID submissionId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        fallbackFeedbackMapper = new FallbackFeedbackMapper();
        submissionId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    void testCreateBasicCodingFeedback_Success() {
        SubmissionEvaluatedEvent event = SubmissionEvaluatedEvent.builder()
                .submissionId(submissionId)
                .userId(userId)
                .score(85)
                .isCorrect(true)
                .overallFeedback("Good job!")
                .testResults(List.of())
                .build();

        CodingSubmissionFeedback result = fallbackFeedbackMapper.createBasicCodingFeedback(event);

        assertNotNull(result);
        assertNotNull(result.getEvaluation());
        
        CodingSubmissionFeedback.Evaluation evaluation = result.getEvaluation();
        assertNotNull(evaluation.getCorrectness());
        assertNotNull(evaluation.getOverall());
        assertNotNull(evaluation.getEfficiency());
        assertNotNull(evaluation.getStyle());
    }

    @Test
    void testCreateBasicCodingFeedback_CorrectnessEvaluation() {
        SubmissionEvaluatedEvent event = SubmissionEvaluatedEvent.builder()
                .submissionId(submissionId)
                .userId(userId)
                .score(92)
                .isCorrect(true)
                .overallFeedback("Excellent solution!")
                .testResults(List.of())
                .build();

        CodingSubmissionFeedback result = fallbackFeedbackMapper.createBasicCodingFeedback(event);

        CodingSubmissionFeedback.CorrectnessEvaluation correctness = result.getEvaluation().getCorrectness();
        assertEquals(92, correctness.getScore());
        assertEquals(92, correctness.getPercentage());
        assertEquals(100, correctness.getMaxScore());
        assertEquals("See test results for correctness.", correctness.getFeedback());
    }

    @Test
    void testCreateBasicCodingFeedback_OverallEvaluation() {
        SubmissionEvaluatedEvent event = SubmissionEvaluatedEvent.builder()
                .submissionId(submissionId)
                .userId(userId)
                .score(75)
                .isCorrect(true)
                .overallFeedback("Good work with room for improvement")
                .testResults(List.of())
                .build();

        CodingSubmissionFeedback result = fallbackFeedbackMapper.createBasicCodingFeedback(event);

        CodingSubmissionFeedback.OverallEvaluation overall = result.getEvaluation().getOverall();
        assertEquals(75, overall.getTotalScore());
        assertEquals(75, overall.getPercentage());
        assertEquals(100, overall.getMaxScore());
        assertEquals("Good work with room for improvement", overall.getSummary());
        assertTrue(overall.isPassed());
        assertEquals(0, overall.getXpEarned());
        assertTrue(overall.getKeyStrengths().isEmpty());
        assertTrue(overall.getKeyImprovements().isEmpty());
    }

    @Test
    void testCreateBasicCodingFeedback_WithTestResults() {
        SubmissionEvaluatedEvent.TestResultData testResult1 = SubmissionEvaluatedEvent.TestResultData.builder()
                .input("1 2")
                .expectedOutput("3")
                .actualOutput("3")
                .passed(true)
                .statusDescription("Test passed")
                .build();

        SubmissionEvaluatedEvent.TestResultData testResult2 = SubmissionEvaluatedEvent.TestResultData.builder()
                .input("5 3")
                .expectedOutput("8")
                .actualOutput("7")
                .passed(false)
                .statusDescription("Output mismatch")
                .build();

        SubmissionEvaluatedEvent event = SubmissionEvaluatedEvent.builder()
                .submissionId(submissionId)
                .userId(userId)
                .score(50)
                .isCorrect(false)
                .overallFeedback("Some tests failed")
                .testResults(List.of(testResult1, testResult2))
                .build();

        CodingSubmissionFeedback result = fallbackFeedbackMapper.createBasicCodingFeedback(event);

        List<CodingSubmissionFeedback.TestResult> testResults = result.getEvaluation().getCorrectness().getTestResults();
        assertEquals(2, testResults.size());

        CodingSubmissionFeedback.TestResult mappedResult1 = testResults.get(0);
        assertEquals("1 2", mappedResult1.getInput());
        assertEquals("3", mappedResult1.getExpectedOutput());
        assertEquals("3", mappedResult1.getActualOutput());
        assertTrue(mappedResult1.isPassed());
        assertEquals("Test passed", mappedResult1.getFeedback());

        CodingSubmissionFeedback.TestResult mappedResult2 = testResults.get(1);
        assertEquals("5 3", mappedResult2.getInput());
        assertFalse(mappedResult2.isPassed());
        assertEquals("Output mismatch", mappedResult2.getFeedback());
    }

    @Test
    void testCreateBasicCodingFeedback_EfficiencyEvaluation() {
        SubmissionEvaluatedEvent event = SubmissionEvaluatedEvent.builder()
                .submissionId(submissionId)
                .userId(userId)
                .score(80)
                .isCorrect(true)
                .overallFeedback("Good code")
                .testResults(List.of())
                .build();

        CodingSubmissionFeedback result = fallbackFeedbackMapper.createBasicCodingFeedback(event);

        CodingSubmissionFeedback.EfficiencyEvaluation efficiency = result.getEvaluation().getEfficiency();
        assertEquals(0, efficiency.getScore());
        assertEquals(0, efficiency.getPercentage());
        assertEquals("N/A", efficiency.getFeedback());
    }

    @Test
    void testCreateBasicCodingFeedback_StyleEvaluation() {
        SubmissionEvaluatedEvent event = SubmissionEvaluatedEvent.builder()
                .submissionId(submissionId)
                .userId(userId)
                .score(80)
                .isCorrect(true)
                .overallFeedback("Good code")
                .testResults(List.of())
                .build();

        CodingSubmissionFeedback result = fallbackFeedbackMapper.createBasicCodingFeedback(event);

        CodingSubmissionFeedback.StyleEvaluation style = result.getEvaluation().getStyle();
        assertEquals(0, style.getScore());
        assertEquals(0, style.getPercentage());
        assertEquals("N/A", style.getFeedback());
    }

    @Test
    void testCreateBasicCodingFeedback_FailedSubmission() {
        SubmissionEvaluatedEvent event = SubmissionEvaluatedEvent.builder()
                .submissionId(submissionId)
                .userId(userId)
                .score(25)
                .isCorrect(false)
                .overallFeedback("Most tests failed")
                .testResults(List.of())
                .build();

        CodingSubmissionFeedback result = fallbackFeedbackMapper.createBasicCodingFeedback(event);

        CodingSubmissionFeedback.OverallEvaluation overall = result.getEvaluation().getOverall();
        assertEquals(25, overall.getTotalScore());
        assertFalse(overall.isPassed());
    }

    @Test
    void testCreateBasicCodingFeedback_ZeroScore() {
        SubmissionEvaluatedEvent event = SubmissionEvaluatedEvent.builder()
                .submissionId(submissionId)
                .userId(userId)
                .score(0)
                .isCorrect(false)
                .overallFeedback("No correct outputs")
                .testResults(List.of())
                .build();

        CodingSubmissionFeedback result = fallbackFeedbackMapper.createBasicCodingFeedback(event);

        assertEquals(0, result.getEvaluation().getCorrectness().getScore());
        assertEquals(0, result.getEvaluation().getOverall().getTotalScore());
    }

    @Test
    void testCreateBasicCodingFeedback_FullScore() {
        SubmissionEvaluatedEvent event = SubmissionEvaluatedEvent.builder()
                .submissionId(submissionId)
                .userId(userId)
                .score(100)
                .isCorrect(true)
                .overallFeedback("Perfect solution!")
                .testResults(List.of())
                .build();

        CodingSubmissionFeedback result = fallbackFeedbackMapper.createBasicCodingFeedback(event);

        assertEquals(100, result.getEvaluation().getCorrectness().getScore());
        assertEquals(100, result.getEvaluation().getOverall().getTotalScore());
        assertTrue(result.getEvaluation().getOverall().isPassed());
    }

    @Test
    void testCreateBasicCodingFeedback_NullTestResults() {
        SubmissionEvaluatedEvent event = SubmissionEvaluatedEvent.builder()
                .submissionId(submissionId)
                .userId(userId)
                .score(85)
                .isCorrect(true)
                .overallFeedback("Good job")
                .testResults(null)
                .build();

        CodingSubmissionFeedback result = fallbackFeedbackMapper.createBasicCodingFeedback(event);

        assertNotNull(result);
        assertTrue(result.getEvaluation().getCorrectness().getTestResults().isEmpty());
    }

    @Test
    void testCreateBasicEssayFeedback_Success() {
        SubmissionEvaluatedEvent event = SubmissionEvaluatedEvent.builder()
                .submissionId(submissionId)
                .userId(userId)
                .score(75)
                .isCorrect(true)
                .overallFeedback("Good essay")
                .build();

        EssaySubmissionFeedback result = fallbackFeedbackMapper.createBasicEssayFeedback(event);

        assertNotNull(result);
        assertEquals(0.0, result.getGrammarScore());
        assertEquals(0.75, result.getRelevanceScore());
        assertEquals("Essay evaluation completed", result.getToneAnalysis());
        assertTrue(result.getSuggestions().isEmpty());
    }

    @Test
    void testCreateBasicEssayFeedback_HighScore() {
        SubmissionEvaluatedEvent event = SubmissionEvaluatedEvent.builder()
                .submissionId(submissionId)
                .userId(userId)
                .score(95)
                .isCorrect(true)
                .overallFeedback("Excellent essay")
                .build();

        EssaySubmissionFeedback result = fallbackFeedbackMapper.createBasicEssayFeedback(event);

        assertEquals(0.95, result.getRelevanceScore());
    }

    @Test
    void testCreateBasicEssayFeedback_LowScore() {
        SubmissionEvaluatedEvent event = SubmissionEvaluatedEvent.builder()
                .submissionId(submissionId)
                .userId(userId)
                .score(30)
                .isCorrect(false)
                .overallFeedback("Needs improvement")
                .build();

        EssaySubmissionFeedback result = fallbackFeedbackMapper.createBasicEssayFeedback(event);

        assertEquals(0.30, result.getRelevanceScore());
    }

    @Test
    void testCreateBasicEssayFeedback_ZeroScore() {
        SubmissionEvaluatedEvent event = SubmissionEvaluatedEvent.builder()
                .submissionId(submissionId)
                .userId(userId)
                .score(0)
                .isCorrect(false)
                .overallFeedback("No relevant content")
                .build();

        EssaySubmissionFeedback result = fallbackFeedbackMapper.createBasicEssayFeedback(event);

        assertEquals(0.0, result.getRelevanceScore());
    }
}
