package com.amalitech.feedback.service.evaluator;

import com.amalitech.common.event.events.SubmissionCreatedEvent;
import com.amalitech.common.event.events.SubmissionEvaluatedEvent;
import com.amalitech.feedback.service.dto.client.TaskDTO;
import com.amalitech.feedback.service.dto.client.request.Judge0SubmissionRequest;
import com.amalitech.feedback.service.dto.client.response.Judge0SubmissionResponse;
import com.amalitech.feedback.service.dto.client.submission.CodingSubmissionFeedback;
import com.amalitech.feedback.service.service.AIFeedbackClient;
import com.amalitech.feedback.service.service.Judge0Client;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Evaluator for CODING tasks.
 * Executes code via Judge0 and generates AI feedback.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CodingTaskEvaluator implements TaskEvaluator {

    private final Judge0Client judge0Client;
    private final AIFeedbackClient aiFeedbackClient;

    @Override
    public Mono<SubmissionEvaluatedEvent> evaluate(SubmissionCreatedEvent event) {
        log.info("Evaluating CODING task submission: {}", event.getSubmissionId());
        
        return runTestCases(event)
                .flatMap(data -> gradeAndProvideFeedback(data.event(), data.results()));
    }

    @Override
    public String getTaskType() {
        return "CODING";
    }

    /**
     * Runs all test cases against Judge0.
     */
    private Mono<EvaluationData> runTestCases(SubmissionCreatedEvent event) {
        List<SubmissionCreatedEvent.TestCaseData> testCases = event.getTestCases();

        if (testCases == null || testCases.isEmpty()) {
            return Mono.error(new RuntimeException("Task " + event.getTaskId() + " has no test cases."));
        }

        return Flux.fromIterable(testCases)
                .concatMap(testCase -> {
                    Judge0SubmissionRequest request = Judge0SubmissionRequest.builder()
                            .languageId(event.getLanguageId())
                            .sourceCode(event.getCodeToEvaluate())
                            .stdin(testCase.getInput())
                            .expectedOutput(testCase.getExpectedOutput())
                            .build();
                    return judge0Client.executeSubmission(request);
                })
                .collectList()
                .map(results -> new EvaluationData(event, results));
    }

    /**
     * Grades the results and generates AI feedback.
     */
    private Mono<SubmissionEvaluatedEvent> gradeAndProvideFeedback(
            SubmissionCreatedEvent event,
            List<Judge0SubmissionResponse> results
    ) {
        int totalTests = results.size();
        int passedTests = (int) results.stream()
                .filter(r -> r.getStatus() != null && r.getStatus().getId() == 3)
                .count();
        boolean isCorrect = totalTests > 0 && passedTests == totalTests;
        int score = isCorrect ? 100 : (int) (((double) passedTests / totalTests) * 100);

        TaskDTO task = buildTaskDTO(event);

        return aiFeedbackClient.generateFeedback(task, event.getCodeToEvaluate(), results)
                .map(aiFeedback -> buildSuccessEvent(event, isCorrect, score, results, aiFeedback))
                .onErrorResume(e -> {
                    log.error("AI feedback generation failed for {}: {}", event.getSubmissionId(), e.getMessage());
                    return Mono.just(buildFallbackEvent(event, isCorrect, score, results));
                });
    }

    /**
     * Builds the evaluated event with AI feedback.
     */
    private SubmissionEvaluatedEvent buildSuccessEvent(
            SubmissionCreatedEvent event,
            boolean isCorrect,
            int score,
            List<Judge0SubmissionResponse> results,
            CodingSubmissionFeedback aiFeedback
    ) {
        String overallFeedback = String.format(
                "Correctness: %s\nEfficiency: %s\nStyle: %s\nSuggestion: %s",
                aiFeedback.getCorrectnessFeedback(),
                aiFeedback.getEfficiencyFeedback(),
                aiFeedback.getStyleFeedback(),
                aiFeedback.getOverallSuggestion()
        );

        List<String> testCaseResults = buildTestCaseResults(results);

        return SubmissionEvaluatedEvent.builder()
                .submissionId(event.getSubmissionId())
                .userId(event.getUserId())
                .status("COMPLETED")
                .score(score)
                .isCorrect(isCorrect)
                .feedbackType("CODING")
                .overallFeedback(overallFeedback)
                .testCaseResults(testCaseResults)
                .build();
    }

    /**
     * Builds a fallback event when AI feedback fails.
     */
    private SubmissionEvaluatedEvent buildFallbackEvent(
            SubmissionCreatedEvent event,
            boolean isCorrect,
            int score,
            List<Judge0SubmissionResponse> results
    ) {
        Judge0SubmissionResponse firstError = results.stream()
                .filter(r -> r.getStatus() != null && r.getStatus().getId() != 3)
                .findFirst()
                .orElse(null);

        String statusDescription = firstError != null && firstError.getStatus() != null
                ? firstError.getStatus().getDescription()
                : (isCorrect ? "All Tests Passed" : "Tests Failed");

        String overallFeedback = "Status: " + statusDescription + ". AI feedback unavailable.";
        List<String> testCaseResults = buildTestCaseResults(results);

        return SubmissionEvaluatedEvent.builder()
                .submissionId(event.getSubmissionId())
                .userId(event.getUserId())
                .status("COMPLETED")
                .score(score)
                .isCorrect(isCorrect)
                .feedbackType("CODING")
                .overallFeedback(overallFeedback)
                .testCaseResults(testCaseResults)
                .build();
    }

    /**
     * Builds test case result descriptions.
     */
    private List<String> buildTestCaseResults(List<Judge0SubmissionResponse> results) {
        return results.stream()
                .map(r -> {
                    if (r.getStatus() != null) {
                        return "Test: " + r.getStatus().getDescription();
                    }
                    return "Test: Error";
                })
                .collect(Collectors.toList());
    }

    /**
     * Builds a minimal TaskDTO from the event for AI feedback.
     */
    private TaskDTO buildTaskDTO(SubmissionCreatedEvent event) {
        return TaskDTO.builder()
                .id(event.getTaskId())
                .description("Coding task for submission " + event.getSubmissionId())
                .build();
    }

    /**
     * Internal record to hold evaluation data.
     */
    private record EvaluationData(SubmissionCreatedEvent event, List<Judge0SubmissionResponse> results) {}
}
