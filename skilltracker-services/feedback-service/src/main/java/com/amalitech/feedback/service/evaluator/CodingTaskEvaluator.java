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
 * 
 * <p>Orchestrates the complete evaluation pipeline for coding submissions:
 * <ol>
 *   <li>Executes code against test cases via Judge0 API</li>
 *   <li>Publishes immediate execution results (for real-time feedback)</li>
 *   <li>Generates detailed AI feedback using OpenAI</li>
 *   <li>Extracts score from AI feedback assessment</li>
 *   <li>Publishes final SubmissionEvaluatedEvent with score and feedback</li>
 * </ol>
 * </p>
 * 
 * <p><strong>Score Calculation:</strong> Uses AI feedback's overall.percentage as the authoritative score,
 * which can evaluate code logic even when test case formatting issues prevent direct output matching.
 * Falls back to Judge0 test calculation if AI feedback generation fails.</p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CodingTaskEvaluator implements TaskEvaluator {

    private final Judge0Client judge0Client;
    private final AIFeedbackClient aiFeedbackClient;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Evaluates a coding submission by executing tests and generating AI feedback.
     * 
     * <p>Pipeline:
     * <ol>
     *   <li>Runs all test cases through Judge0</li>
     *   <li>Publishes SubmissionExecutedEvent with immediate results</li>
     *   <li>Generates AI feedback and publishes SubmissionEvaluatedEvent with final score</li>
     * </ol>
     * </p>
     * 
     * @param event the submission to evaluate
     * @return Mono emitting SubmissionEvaluatedEvent with score, feedback, and test results
     */
    @Override
    public Mono<SubmissionEvaluatedEvent> evaluate(SubmissionCreatedEvent event) {
        log.info("Evaluating CODING task submission: {}", event.getSubmissionId());

        return runTestCases(event)
                .doOnNext(data -> publishExecutionResults(event, data.results()))
                .flatMap(data -> gradeAndProvideFeedback(data.event(), data.results()));
    }

    /**
     * Returns the task type this evaluator handles.
     * 
     * @return the task type identifier ("CODING")
     */
    @Override
    public String getTaskType() {
        return "CODING";
    }

    /**
     * Normalizes a string by removing newlines, carriage returns, and trimming whitespace.
     * Used for comparing expected vs actual test outputs.
     * 
     * @param s the string to normalize (may be null)
     * @return normalized string, or empty string if input is null
     */
    private String normalize(String s) {
        if (s == null) return "";
        return s.replace("\r", "").replace("\n", "").trim();
    }

    /**
     * Executes all test cases against the submitted code via Judge0.
     * 
     * <p>Each test case is submitted separately to Judge0 with the user's source code
     * and test input. Expected outputs are NOT sent to Judge0; comparison happens locally.</p>
     * 
     * @param event the submission event containing code and test cases
     * @return Mono emitting EvaluationData with execution results for all tests
     * @throws InvalidTaskException if the task has no test cases
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
                            .expectedOutput(null)
                            .build();
                    return judge0Client.executeSubmission(request);
                })
                .collectList()
                .map(results -> new EvaluationData(event, results));
    }

    /**
     * Grades the submission and generates detailed AI feedback.
     * 
     * <p>Calls the AI feedback service to generate a comprehensive evaluation including
     * correctness, efficiency, and style assessments. Uses AI's overall percentage as the
     * final score. If AI generation fails, falls back to a basic score calculated from
     * Judge0 test results.</p>
     * 
     * @param event the submission event
     * @param results Judge0 execution results for all test cases
     * @return Mono emitting SubmissionEvaluatedEvent with AI feedback and calculated score
     */
    private Mono<SubmissionEvaluatedEvent> gradeAndProvideFeedback(
            SubmissionCreatedEvent event,
            List<Judge0SubmissionResponse> results
    ) {
        List<SubmissionCreatedEvent.TestCaseData> testCases = event.getTestCases();

        List<CommonTestResult> commonResults = buildCommonTestResults(testCases, results);

        int totalTests = commonResults.size();
        int passedTests = (int) commonResults.stream()
                .filter(CommonTestResult::passed)
                .count();
        int fallbackScore = totalTests > 0 ? (int) (((double) passedTests / totalTests) * 100) : 0;
        boolean fallbackIsCorrect = fallbackScore >= 70;

        TaskDTO task = buildTaskDTO(event);

        return aiFeedbackClient.generateDetailedFeedback(task, event.getContentToEvaluate(), results)
                .map(aiFeedback -> {
                    int aiScore = extractScoreFromAIFeedback(aiFeedback);
                    boolean aiIsCorrect = aiScore >= 70;

                    return buildSuccessEvent(event, aiIsCorrect, aiScore, results, aiFeedback);
                })
                .onErrorResume(e -> {
                    log.error("AI DETAILED feedback generation failed for {}: {}", event.getSubmissionId(), e.getMessage(), e);
                    log.info("Using fallback event with isCorrect={}, score={}", fallbackIsCorrect, fallbackScore);
                    return Mono.just(buildFallbackEvent(event, fallbackIsCorrect, fallbackScore, results));
                });
    }

    /**
     * Extracts the overall percentage score from AI feedback.
     * 
     * <p>Retrieves the evaluation.overall.percentage field which represents the AI's
     * assessment of the submission quality. This is used as the authoritative score
     * for the submission.</p>
     * 
     * @param aiFeedback the AI-generated feedback response
     * @return the overall percentage score (0-100), or 0 if feedback is invalid
     */
    private int extractScoreFromAIFeedback(DetailedEvaluationResponse aiFeedback) {
        if (aiFeedback == null || aiFeedback.getEvaluation() == null) {
            return 0;
        }

        DetailedEvaluationResponse.Evaluation eval = aiFeedback.getEvaluation();
        if (eval.getOverall() != null && eval.getOverall().getPercentage() != null) {
            return eval.getOverall().getPercentage();
        }

        return 0;
    }

    /**
     * Builds SubmissionEvaluatedEvent with AI feedback.
     * 
     * <p>Creates the final event with detailed feedback and AI assessment. Falls back to
     * buildFallbackEvent if AI feedback is malformed or incomplete.</p>
     * 
     * @param event the original submission event
     * @param isCorrect whether the submission meets the passing threshold
     * @param score the overall score (0-100)
     * @param results Judge0 execution results
     * @param aiFeedback detailed AI feedback response
     * @return SubmissionEvaluatedEvent with complete feedback data
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
     * Serializes AI feedback to JSON string for storage and transmission.
     * 
     * @param feedback the DetailedEvaluationResponse to serialize
     * @return JSON string representation, or null if serialization fails
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
     * Builds SubmissionEvaluatedEvent without AI feedback.
     * 
     * <p>Used when AI feedback generation fails. Constructs a response with basic status
     * information derived from Judge0 execution results.</p>
     * 
     * @param event the original submission event
     * @param isCorrect whether the submission meets the passing threshold
     * @param score the overall score (0-100) based on test case pass/fail
     * @param results Judge0 execution results
     * @return SubmissionEvaluatedEvent with fallback feedback
     */
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

    /**
     * Builds the common base of SubmissionEvaluatedEvent.
     * 
     * <p>Constructs the shared builder with standard fields including status, score, test results,
     * execution metrics, and stdout/stderr. This builder is customized by calling code to add
     * feedback-specific fields (overallFeedback, detailedFeedback).</p>
     * 
     * @param event the original submission event
     * @param isCorrect whether the submission meets the passing threshold
     * @param score the overall score (0-100)
     * @param results Judge0 execution results for all test cases
     * @return SubmissionEvaluatedEventBuilder with common fields pre-populated
     */
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
     * Builds structured test result data for the SubmissionEvaluatedEvent.
     * 
     * <p>Converts combined test case and Judge0 execution data into the structured format
     * required by SubmissionEvaluatedEvent, including pass/fail status and execution metrics.</p>
     * 
     * @param testCases the original test case definitions from the task
     * @param results Judge0 execution results for all test cases
     * @return list of structured TestResultData objects with complete test information
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
     * Builds TaskDTO from submission event data.
     * 
     * @param event the submission event containing task information
     * @return TaskDTO with task ID and description
     */
    private TaskDTO buildTaskDTO(SubmissionCreatedEvent event) {
        return TaskDTO.builder()
                .id(event.getTaskId())
                .description("Coding task for submission " + event.getSubmissionId())
                .build();
    }

    /**
     * Publishes immediate execution results via SubmissionExecutedEvent.
     * 
     * <p>Sends aggregated test results and execution metrics to RabbitMQ for real-time feedback
     * before AI feedback is generated. This allows consumers to provide early feedback to users.</p>
     * 
     * @param event the original submission event
     * @param results Judge0 execution results for all test cases
     */
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
     * Aggregates test results and calculates cumulative metrics.
     * 
     * <p>Builds the TestResultData list and calculates aggregates (passedCount, avgTime, avgMemory)
     * in a single pass over the test results. Used by publishExecutionResults to provide
     * real-time execution feedback.</p>
     * 
     * @param testCases the original test case definitions from the task
     * @param results Judge0 execution results for all test cases
     * @return AggregatedTestResults containing structured test data and computed metrics
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

    /**
     * Builds common test result objects from test cases and Judge0 responses.
     * 
     * <p>Normalizes expected and actual outputs, compares them for correctness, and extracts
     * execution metrics. Handles cases where test case or execution result data is missing.</p>
     * 
     * @param testCases the original test case definitions from the task
     * @param results Judge0 execution responses for submitted code
     * @return list of CommonTestResult objects with normalized outputs and pass/fail status
     */
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


    /**
     * Encapsulates submission event and Judge0 execution results.
     * 
     * @param event the submission event
     * @param results Judge0 execution responses for all test cases
     */
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

    /**
     * Represents a normalized test result with execution metrics.
     * 
     * @param passed whether the test case passed (normalized output matches expected output)
     * @param input the test input provided to the program
     * @param expectedOutput the expected output (normalized)
     * @param actualOutput the actual program output (normalized)
     * @param execTime the execution time in milliseconds, or null if unavailable
     * @param memory the memory used in KB, or null if unavailable
     * @param statusDesc the status description from Judge0 (e.g., "Accepted", "Wrong Answer")
     */
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