package com.amalitech.feedback.service.evaluator;

import com.amalitech.common.event.events.SubmissionCreatedEvent;
import com.amalitech.common.event.events.SubmissionEvaluatedEvent;
import com.amalitech.feedback.service.dto.client.TaskDTO;
import com.amalitech.feedback.service.dto.client.request.Judge0SubmissionRequest;
import com.amalitech.feedback.service.dto.client.response.Judge0SubmissionResponse;
import com.amalitech.feedback.service.dto.client.submission.impl.CodingSubmissionFeedback;
import com.amalitech.feedback.service.service.AIFeedbackClient;
import com.amalitech.feedback.service.service.Judge0Client;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

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

        Judge0SubmissionResponse firstResult = results.isEmpty() ? null : results.get(0);
        String stdout = firstResult != null ? firstResult.getStdout() : null;
        String stderr = firstResult != null ? firstResult.getStderr() : null;

        List<SubmissionEvaluatedEvent.TestResultData> structuredTestResults = buildStructuredTestResults(
                event.getTestCases(), 
                results
        );

        double avgTime = results.stream()
                .filter(r -> r.getTime() != null)
                .mapToDouble(Judge0SubmissionResponse::getTime)
                .average()
                .orElse(0.0) * 1000;

        int avgMemory = (int) results.stream()
                .filter(r -> r.getMemory() != null)
                .mapToInt(Judge0SubmissionResponse::getMemory)
                .average()
                .orElse(0.0);

        return SubmissionEvaluatedEvent.builder()
                .submissionId(event.getSubmissionId())
                .userId(event.getUserId())
                .status("COMPLETED")
                .score(score)
                .isCorrect(isCorrect)
                .feedbackType("CODING")
                .overallFeedback(overallFeedback)
                .stdout(stdout)
                .stderr(stderr)
                .testResults(structuredTestResults)
                .avgExecutionTimeMs(avgTime)
                .avgMemoryUsedKb(avgMemory)
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
        
        Judge0SubmissionResponse firstResult = results.isEmpty() ? null : results.get(0);
        String stdout = firstResult != null ? firstResult.getStdout() : null;
        String stderr = firstResult != null ? firstResult.getStderr() : null;

        List<SubmissionEvaluatedEvent.TestResultData> structuredTestResults = buildStructuredTestResults(
                event.getTestCases(), 
                results
        );

        double avgTime = results.stream()
                .filter(r -> r.getTime() != null)
                .mapToDouble(Judge0SubmissionResponse::getTime)
                .average()
                .orElse(0.0) * 1000;

        int avgMemory = (int) results.stream()
                .filter(r -> r.getMemory() != null)
                .mapToInt(Judge0SubmissionResponse::getMemory)
                .average()
                .orElse(0.0);

        return SubmissionEvaluatedEvent.builder()
                .submissionId(event.getSubmissionId())
                .userId(event.getUserId())
                .status("COMPLETED")
                .score(score)
                .isCorrect(isCorrect)
                .feedbackType("CODING")
                .overallFeedback(overallFeedback)
                .stdout(stdout)
                .stderr(stderr)
                .testResults(structuredTestResults)
                .avgExecutionTimeMs(avgTime)
                .avgMemoryUsedKb(avgMemory)
                .build();
    }

    /**
     * Builds structured test case results with detailed information.
     */
    private List<SubmissionEvaluatedEvent.TestResultData> buildStructuredTestResults(
            List<SubmissionCreatedEvent.TestCaseData> testCases,
            List<Judge0SubmissionResponse> results
    ) {
        List<SubmissionEvaluatedEvent.TestResultData> structuredResults = new java.util.ArrayList<>();
        
        for (int i = 0; i < results.size(); i++) {
            Judge0SubmissionResponse result = results.get(i);
            SubmissionCreatedEvent.TestCaseData testCase = i < testCases.size() ? testCases.get(i) : null;
            
            boolean passed = result.getStatus() != null && result.getStatus().getId() == 3;
            String statusDesc = result.getStatus() != null ? result.getStatus().getDescription() : "Unknown";
            Long execTime = result.getTime() != null ? (long)(result.getTime() * 1000) : null;
            Integer memory = result.getMemory();
            
            SubmissionEvaluatedEvent.TestResultData testResult = SubmissionEvaluatedEvent.TestResultData.builder()
                    .passed(passed)
                    .input(testCase != null ? testCase.getInput() : "")
                    .expectedOutput(testCase != null ? testCase.getExpectedOutput() : "")
                    .actualOutput(result.getStdout() != null ? result.getStdout() : "")
                    .executionTimeMs(execTime)
                    .memoryUsedKb(memory)
                    .statusDescription(statusDesc)
                    .build();
            
            structuredResults.add(testResult);
        }
        
        return structuredResults;
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
