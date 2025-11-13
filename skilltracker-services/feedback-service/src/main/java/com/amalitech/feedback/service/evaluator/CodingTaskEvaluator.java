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

    /**
     * Runs all test cases against Judge0.
     */
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

        return aiFeedbackClient.generateDetailedFeedback(task, event.getContentToEvaluate(), results)
                .map(aiFeedback -> buildSuccessEvent(event, isCorrect, score, results, aiFeedback))
                .onErrorResume(e -> {
                    log.error("AI DETAILED feedback generation failed for {}: {}", event.getSubmissionId(), e.getMessage());
                    return Mono.just(buildFallbackEvent(event, isCorrect, score, results));
                });
    }

    /**
     * Builds the evaluated event with AI feedback.
     * <p>
     * This method validates that the feedback structure contains all required
     * evaluation components. If any required field is null, it logs a warning
     * and falls back to a basic event without detailed feedback.
     */
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

    /**
     * Serializes the detailed feedback to JSON, adding the polymorphic key.
     */
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

        return buildCommonEvent(event, isCorrect, score, results)
                .overallFeedback(overallFeedback)
                .detailedFeedback(null)
                .build();
    }

    /**
     * This method contains all the duplicated logic.
     * It builds and returns a pre-populated EventBuilder.
     */
    private SubmissionEvaluatedEventBuilder buildCommonEvent(
            SubmissionCreatedEvent event,
            boolean isCorrect,
            int score,
            List<Judge0SubmissionResponse> results)
    {
        Judge0SubmissionResponse firstResult = results.isEmpty() ? null : results.get(0);

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

        return SubmissionEvaluatedEvent.builder()
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
    }

    /**
     * Builds structured test case results with detailed information.
     */
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
     * Publishes execution results immediately (before AI evaluation).
     * This provides fast feedback to users showing code output and test results.
     */
    private void publishExecutionResults(SubmissionCreatedEvent event, List<Judge0SubmissionResponse> results) {
        log.info("Publishing immediate execution results for submission: {}", event.getSubmissionId());
        
        Judge0SubmissionResponse firstResult = results.isEmpty() ? null : results.get(0);
        String stdout = firstResult != null ? firstResult.getStdout() : null;
        String stderr = firstResult != null ? firstResult.getStderr() : null;
        
        int passedCount = (int) results.stream()
                .filter(r -> r.getStatus() != null && r.getStatus().getId() == 3)
                .count();
        boolean allPassed = !results.isEmpty() && passedCount == results.size();
        
        List<SubmissionExecutedEvent.TestResultData> testResults = buildExecutedTestResults(
                event.getTestCases(), 
                results
        );
        
        double avgTime;
        avgTime = results.stream()
                .filter(r -> r.getTime() != null)
                .mapToDouble(Judge0SubmissionResponse::getTime)
                .average()
                .orElse(0.0) * 1000;

        int avgMemory = (int) results.stream()
                .filter(r -> r.getMemory() != null)
                .mapToInt(Judge0SubmissionResponse::getMemory)
                .average()
                .orElse(0.0);
        
        SubmissionExecutedEvent executedEvent = SubmissionExecutedEvent.builder()
                .submissionId(event.getSubmissionId())
                .userId(event.getUserId())
                .stdout(stdout)
                .stderr(stderr)
                .testResults(testResults)
                .allTestsPassed(allPassed)
                .testsPassed(passedCount)
                .testsTotal(results.size())
                .avgExecutionTimeMs(avgTime)
                .avgMemoryUsedKb(avgMemory)
                .build();
        
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SUBMISSION_EXCHANGE,
                RabbitMQConfig.SUBMISSION_EXECUTED_ROUTING_KEY,
                executedEvent
        );
        
        log.info("Published execution results for submission: {}", event.getSubmissionId());
    }

    /**
     * Builds structured test results for SubmissionExecutedEvent.
     */
    private List<SubmissionExecutedEvent.TestResultData> buildExecutedTestResults(
            List<SubmissionCreatedEvent.TestCaseData> testCases,
            List<Judge0SubmissionResponse> results
    ) {
        List<CommonTestResult> commonResults = buildCommonTestResults(testCases, results);

        return commonResults.stream()
                .map(r -> SubmissionExecutedEvent.TestResultData.builder()
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

    /**
     * Contains the shared logic that was previously in two methods.
     * It loops through results and builds a list of common, intermediate objects.
     */
    private List<CommonTestResult> buildCommonTestResults(
            List<SubmissionCreatedEvent.TestCaseData> testCases,
            List<Judge0SubmissionResponse> results
    ) {
        List<CommonTestResult> commonResults = new java.util.ArrayList<>();

        for (int i = 0; i < results.size(); i++) {
            Judge0SubmissionResponse result = results.get(i);
            SubmissionCreatedEvent.TestCaseData testCase = i < testCases.size() ? testCases.get(i) : null;

            boolean passed = result.getStatus() != null && result.getStatus().getId() == 3;
            String statusDesc = result.getStatus() != null ? result.getStatus().getDescription() : "Unknown";
            Long execTime = result.getTime() != null ? (long)(result.getTime() * 1000) : null;
            Integer memory = result.getMemory();
            String input = testCase != null ? testCase.getInput() : "";
            String expected = testCase != null ? testCase.getExpectedOutput() : "";
            String actual = result.getStdout() != null ? result.getStdout() : "";

            commonResults.add(new CommonTestResult(
                    passed, input, expected, actual, execTime, memory, statusDesc
            ));
        }
        return commonResults;
    }

    /**
     * Internal record to hold evaluation data.
     */
    private record EvaluationData(SubmissionCreatedEvent event, List<Judge0SubmissionResponse> results) {}

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
