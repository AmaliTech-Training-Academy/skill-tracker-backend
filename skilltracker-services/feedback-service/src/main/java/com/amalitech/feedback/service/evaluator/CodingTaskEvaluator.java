package com.amalitech.feedback.service.evaluator;

import com.amalitech.common.event.events.SubmissionCreatedEvent;
import com.amalitech.common.event.events.SubmissionEvaluatedEvent;
import com.amalitech.common.event.events.SubmissionEvaluatedEvent.SubmissionEvaluatedEventBuilder;
import com.amalitech.common.event.events.SubmissionExecutedEvent;
import com.amalitech.feedback.service.config.RabbitMQConfig;
import com.amalitech.feedback.service.dto.client.TaskDTO;
import com.amalitech.feedback.service.dto.client.request.Judge0SubmissionRequest;
import com.amalitech.feedback.service.dto.client.response.Judge0SubmissionResponse;
import com.amalitech.feedback.service.dto.client.submission.DetailedEvaluationResponse;
import com.amalitech.feedback.service.exception.InvalidTaskException;
import com.amalitech.feedback.service.service.AIFeedbackClient;
import com.amalitech.feedback.service.service.Judge0Client;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<SubmissionEvaluatedEvent> evaluate(SubmissionCreatedEvent event) {
        log.info("Evaluating CODING task submission: {}", event.getSubmissionId());

        return runTestCases(event)
                .doOnNext(data -> publishExecutionResults(event, data.results()))
                .flatMap(data -> gradeAndProvideFeedback(data.event(), data.results()));
    }

    @Override
    public String getTaskType() {
        return "CODING";
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.replace("\r", "").replace("\n", "").trim();
    }

    private Mono<EvaluationData> runTestCases(SubmissionCreatedEvent event) {
        List<SubmissionCreatedEvent.TestCaseData> testCases = event.getTestCases();

        if (testCases == null || testCases.isEmpty()) {
            return Mono.error(new InvalidTaskException("Task " + event.getTaskId() + " has no test cases."));
        }

        return Flux.fromIterable(testCases)
                .concatMap(testCase -> {
                    Judge0SubmissionRequest request = Judge0SubmissionRequest.builder()
                            .languageId(event.getLanguageId())
                            .sourceCode(event.getContentToEvaluate())
                            .stdin(testCase.getInput())
                            .expectedOutput(null)
                            .build();
                    return judge0Client.executeSubmission(request);
                })
                .collectList()
                .map(results -> new EvaluationData(event, results));
    }

    private Mono<SubmissionEvaluatedEvent> gradeAndProvideFeedback(
            SubmissionCreatedEvent event,
            List<Judge0SubmissionResponse> results
    ) {
        List<SubmissionCreatedEvent.TestCaseData> testCases = event.getTestCases();
        log.info("gradeAndProvideFeedback START - submission: {}, testCases: {}", event.getSubmissionId(), testCases != null ? testCases.size() : 0);
        
        List<CommonTestResult> commonResults = buildCommonTestResults(testCases, results);
        log.info("Built commonResults: {} results", commonResults.size());

        int totalTests = commonResults.size();
        int passedTests = (int) commonResults.stream()
                .filter(CommonTestResult::passed)
                .count();

        int score = totalTests > 0 ? (int) (((double) passedTests / totalTests) * 100) : 0;
        boolean isCorrect = score >= 70;
        
        log.info("Score calculation: totalTests={}, passedTests={}, score={}, isCorrect={}", 
                 totalTests, passedTests, score, isCorrect);

        TaskDTO task = buildTaskDTO(event);

        return aiFeedbackClient.generateDetailedFeedback(task, event.getContentToEvaluate(), results)
                .map(aiFeedback -> {
                    log.info("Successfully got AI feedback for {}", event.getSubmissionId());
                    return buildSuccessEvent(event, isCorrect, score, results, aiFeedback);
                })
                .onErrorResume(e -> {
                    log.error("AI DETAILED feedback generation failed for {}: {}", event.getSubmissionId(), e.getMessage(), e);
                    log.info("Using fallback event with isCorrect={}, score={}", isCorrect, score);
                    return Mono.just(buildFallbackEvent(event, isCorrect, score, results));
                });
    }

    private SubmissionEvaluatedEvent buildSuccessEvent(
            SubmissionCreatedEvent event,
            boolean isCorrect,
            int score,
            List<Judge0SubmissionResponse> results,
            DetailedEvaluationResponse aiFeedback
    ) {
        if (aiFeedback == null || aiFeedback.getEvaluation() == null) {
            log.warn("Feedback or evaluation is null for submission: {}", event.getSubmissionId());
            return buildFallbackEvent(event, isCorrect, score, results);
        }

        DetailedEvaluationResponse.Evaluation eval = aiFeedback.getEvaluation();
        if (eval.getOverall() == null) {
            log.warn("Overall evaluation is null for submission: {}", event.getSubmissionId());
            return buildFallbackEvent(event, isCorrect, score, results);
        }

        String overallFeedback = eval.getOverall().getSummary() != null
                ? eval.getOverall().getSummary()
                : "Code evaluation completed.";
        String detailedFeedback = serializeDetailedFeedback(aiFeedback);

        return buildCommonEvent(event, isCorrect, score, results)
                .overallFeedback(overallFeedback)
                .detailedFeedback(detailedFeedback)
                .build();
    }

    private String serializeDetailedFeedback(DetailedEvaluationResponse feedback) {
        try {
            Map<String, Object> polymorphicFeedback = new HashMap<>();
            polymorphicFeedback.put("feedbackType", "CODING");
            polymorphicFeedback.put("evaluation", feedback.getEvaluation());
            return objectMapper.writeValueAsString(polymorphicFeedback);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize detailed CODING feedback map: {}", e.getMessage());
            return null;
        }
    }

    private SubmissionEvaluatedEvent buildFallbackEvent(
            SubmissionCreatedEvent event,
            boolean isCorrect,
            int score,
            List<Judge0SubmissionResponse> results
    ) {
        Judge0SubmissionResponse firstError = results.stream()
                .filter(r -> r.getStatus() == null || r.getStatus().getId() != 3)
                .findFirst()
                .orElse(null);

        String statusDescription = firstError != null && firstError.getStatus() != null
                ? firstError.getStatus().getDescription()
                : (isCorrect ? "All Tests Passed" : "Tests Failed");

        if (firstError == null && !isCorrect) {
            statusDescription = "Wrong Answer";
        }

        String overallFeedback = "Status: " + statusDescription + ". AI feedback unavailable.";

        return buildCommonEvent(event, isCorrect, score, results)
                .overallFeedback(overallFeedback)
                .detailedFeedback(null)
                .build();
    }

    private SubmissionEvaluatedEventBuilder buildCommonEvent(
            SubmissionCreatedEvent event,
            boolean isCorrect,
            int score,
            List<Judge0SubmissionResponse> results)
    {
        Judge0SubmissionResponse firstResult = results.isEmpty() ? null : results.getFirst();

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

        List<SubmissionEvaluatedEvent.TestResultData> structuredTestResults = buildStructuredTestResults(
                event.getTestCases(),
                results
        );

        log.info("Building event for submission {}: score={}, isCorrect={}", event.getSubmissionId(), score, isCorrect);
        
        SubmissionEvaluatedEvent.SubmissionEvaluatedEventBuilder builder = SubmissionEvaluatedEvent.builder()
                .submissionId(event.getSubmissionId())
                .userId(event.getUserId())
                .status("COMPLETED")
                .score(score)
                .isCorrect(isCorrect)
                .feedbackType("CODING")
                .stdout(firstResult != null ? firstResult.getStdout() : null)
                .stderr(firstResult != null ? firstResult.getStderr() : null)
                .testResults(structuredTestResults)
                .avgExecutionTimeMs(avgTime)
                .avgMemoryUsedKb(avgMemory);
        
        log.debug("Builder state - score={}, isCorrect={}", score, isCorrect);
        return builder;
    }

    private List<SubmissionEvaluatedEvent.TestResultData> buildStructuredTestResults(
            List<SubmissionCreatedEvent.TestCaseData> testCases,
            List<Judge0SubmissionResponse> results
    ) {
        List<CommonTestResult> commonResults = buildCommonTestResults(testCases, results);

        return commonResults.stream()
                .map(r -> SubmissionEvaluatedEvent.TestResultData.builder()
                        .passed(r.passed())
                        .input(r.input())
                        .expectedOutput(r.expectedOutput())
                        .actualOutput(r.actualOutput())
                        .executionTimeMs(r.execTime())
                        .memoryUsedKb(r.memory())
                        .statusDescription(r.statusDesc())
                        .build())
                .collect(Collectors.toList());
    }

    private TaskDTO buildTaskDTO(SubmissionCreatedEvent event) {
        return TaskDTO.builder()
                .id(event.getTaskId())
                .description("Coding task for submission " + event.getSubmissionId())
                .build();
    }

    private void publishExecutionResults(SubmissionCreatedEvent event, List<Judge0SubmissionResponse> results) {
        log.info("Publishing immediate execution results for submission: {}", event.getSubmissionId());

        Judge0SubmissionResponse firstResult = results.isEmpty() ? null : results.get(0);
        String stdout = firstResult != null ? firstResult.getStdout() : null;
        String stderr = firstResult != null ? firstResult.getStderr() : null;

        AggregatedTestResults aggregated = aggregateTestResults(event.getTestCases(), results);

        int totalTests = aggregated.testResults().size();
        boolean allPassed = !aggregated.testResults().isEmpty() && (aggregated.passedCount() == totalTests);

        SubmissionExecutedEvent executedEvent = SubmissionExecutedEvent.builder()
                .submissionId(event.getSubmissionId())
                .userId(event.getUserId())
                .stdout(stdout)
                .stderr(stderr)
                .testResults(aggregated.testResults())
                .allTestsPassed(allPassed)
                .testsPassed(aggregated.passedCount())
                .testsTotal(totalTests)
                .avgExecutionTimeMs(aggregated.avgTimeMs())
                .avgMemoryUsedKb(aggregated.avgMemoryKb())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SUBMISSION_EXCHANGE,
                RabbitMQConfig.SUBMISSION_EXECUTED_ROUTING_KEY,
                executedEvent
        );

        log.info("Published execution results for submission: {}", event.getSubmissionId());
    }

    /**
     * Builds the TestResultData list AND calculates all aggregates
     * (passedCount, avgTime, avgMemory) in a single pass.
     */
    private AggregatedTestResults aggregateTestResults(
            List<SubmissionCreatedEvent.TestCaseData> testCases,
            List<Judge0SubmissionResponse> results
    ) {
        List<CommonTestResult> commonResults = buildCommonTestResults(testCases, results);

        List<SubmissionExecutedEvent.TestResultData> testResultsList = new java.util.ArrayList<>();
        int passedCount = 0;
        double totalTimeMs = 0;
        int totalMemoryKb = 0;
        int timeRecordCount = 0;
        int memoryRecordCount = 0;

        for (CommonTestResult r : commonResults) {
            testResultsList.add(SubmissionExecutedEvent.TestResultData.builder()
                    .passed(r.passed())
                    .input(r.input())
                    .expectedOutput(r.expectedOutput())
                    .actualOutput(r.actualOutput())
                    .executionTimeMs(r.execTime())
                    .memoryUsedKb(r.memory())
                    .statusDescription(r.statusDesc())
                    .build());

            if (r.passed()) {
                passedCount++;
            }
            if (r.execTime() != null) {
                totalTimeMs += r.execTime();
                timeRecordCount++;
            }
            if (r.memory() != null) {
                totalMemoryKb += r.memory();
                memoryRecordCount++;
            }
        }

        double avgTimeMs = (timeRecordCount > 0) ? (totalTimeMs / timeRecordCount) : 0.0;
        int avgMemoryKb = (memoryRecordCount > 0) ? (totalMemoryKb / memoryRecordCount) : 0;

        return new AggregatedTestResults(testResultsList, passedCount, avgTimeMs, avgMemoryKb);
    }

    private List<CommonTestResult> buildCommonTestResults(
            List<SubmissionCreatedEvent.TestCaseData> testCases,
            List<Judge0SubmissionResponse> results
    ) {
        List<CommonTestResult> commonResults = new java.util.ArrayList<>();

        for (int i = 0; i < results.size(); i++) {
            Judge0SubmissionResponse result = results.get(i);
            SubmissionCreatedEvent.TestCaseData testCase = i < testCases.size() ? testCases.get(i) : null;

            String expected = testCase != null ? normalize(testCase.getExpectedOutput()) : "";
            String actual = result.getStdout() != null ? normalize(result.getStdout()) : "";
            String input = testCase != null ? testCase.getInput() : "";

            boolean isExecuted = result.getStatus() != null && result.getStatus().getId() == 3;
            boolean isCorrect = isExecuted && expected.equals(actual);

            String statusDesc = result.getStatus() != null ? result.getStatus().getDescription() : "Unknown";

            if (isExecuted && !isCorrect) {
                statusDesc = "Wrong Answer";
            }

            Long execTime = result.getTime() != null ? (long)(result.getTime() * 1000) : null;

            Integer memory = result.getMemory();

            commonResults.add(new CommonTestResult(
                    isCorrect, input, expected, actual, execTime, memory, statusDesc
            ));
        }
        return commonResults;
    }


    private record EvaluationData(
            SubmissionCreatedEvent event,
            List<Judge0SubmissionResponse> results)
    {}

    /**
     * Internal record to hold all aggregated results from a single pass
     * over the test case results.
     */
    private record AggregatedTestResults(
            List<SubmissionExecutedEvent.TestResultData> testResults,
            int passedCount,
            double avgTimeMs,
            int avgMemoryKb
    ) {}

    private record CommonTestResult(
            boolean passed,
            String input,
            String expectedOutput,
            String actualOutput,
            Long execTime,
            Integer memory,
            String statusDesc
    ) {}
}