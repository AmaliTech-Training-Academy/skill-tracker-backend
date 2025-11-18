package com.amalitech.task.service.service;

import com.amalitech.task.service.dto.client.request.Judge0SubmissionRequest;
import com.amalitech.task.service.dto.client.response.Judge0SubmissionResponse;
import com.amalitech.task.service.dto.response.RunCodeResponse;
import com.amalitech.task.service.model.Task;
import com.amalitech.task.service.model.content.impl.CodingTaskContent;
import com.amalitech.task.service.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of CodeExecutionService.
 * Orchestrates the code execution flow by fetching task details,
 * executing code via Judge0, normalizing outputs, and building structured responses.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CodeExecutionProcessor implements CodeExecutionService {

    private final Judge0Client judge0Client;
    private final TaskRepository taskRepository;

    /**
     * Executes code against all test cases for a given task.
     *
     * @param taskId the ID of the task
     * @param code the source code to execute
     * @param languageId the Judge0 language ID
     * @return Mono containing the execution results
     */
    public Mono<RunCodeResponse> executeCode(UUID taskId, String code, Integer languageId) {
        return Mono.fromCallable(() -> taskRepository.findById(taskId))
                .flatMap(taskOptional -> {
                    return taskOptional.<Mono<? extends Task>>map(Mono::just).orElseGet(() -> Mono.error(new IllegalArgumentException("Task not found: " + taskId)));
                })
                .flatMap(task -> executeTestCases(task, code, languageId));
    }

    /**
     * Executes all test cases for a task and builds the response.
     *
     * @param task the task entity containing test cases
     * @param code the source code to execute
     * @param languageId the Judge0 language ID
     * @return Mono containing the structured execution results
     */
    private Mono<RunCodeResponse> executeTestCases(Task task, String code, Integer languageId) {
        if (!(task.getContent() instanceof CodingTaskContent)) {
            return Mono.error(new IllegalArgumentException("Task is not a coding task"));
        }

        CodingTaskContent content = (CodingTaskContent) task.getContent();
        List<CodingTaskContent.TestCase> testCases = content.getTestCases();

        if (testCases == null || testCases.isEmpty()) {
            return Mono.error(new IllegalArgumentException("Task has no test cases"));
        }

        log.info("Executing {} test cases for task: {}", testCases.size(), task.getId());

        // Execute each test case against Judge0
        return Flux.fromIterable(testCases)
                .concatMap(testCase -> {
                    Judge0SubmissionRequest request = Judge0SubmissionRequest.builder()
                            .languageId(languageId)
                            .sourceCode(code)
                            .stdin(testCase.getInput())
                            .expectedOutput(null)
                            .build();
                    return judge0Client.executeSubmission(request)
                            .map(result -> new TestExecutionResult(testCase, result));
                })
                .collectList()
                .map(results -> buildResponse(results))
                .doOnSuccess(response -> log.info("Code execution completed. Tests passed: {}/{}", 
                        response.getTestsPassed(), response.getTestsTotal()));
    }

    /**
     * Builds the structured RunCodeResponse from execution results.
     *
     * @param results list of test execution results
     * @return RunCodeResponse with test results and aggregated metrics
     */
    private RunCodeResponse buildResponse(List<TestExecutionResult> results) {
        List<RunCodeResponse.TestResultData> testResults = new ArrayList<>();
        int passedCount = 0;
        double totalTimeMs = 0;
        int totalMemoryKb = 0;
        int timeRecordCount = 0;
        int memoryRecordCount = 0;
        String stdout = null;
        String stderr = null;

        for (int i = 0; i < results.size(); i++) {
            TestExecutionResult result = results.get(i);
            Judge0SubmissionResponse judge0Response = result.judge0Response;
            CodingTaskContent.TestCase testCase = result.testCase;

            String expectedOutput = normalize(testCase.getExpectedOutput());
            String actualOutput = judge0Response.getStdout() != null ? 
                    normalize(judge0Response.getStdout()) : "";

            boolean isExecuted = judge0Response.getStatus() != null && 
                    judge0Response.getStatus().getId() == 3; // Status 3 = Accepted
            boolean passed = isExecuted && expectedOutput.equals(actualOutput);

            String statusDescription = judge0Response.getStatus() != null ? 
                    judge0Response.getStatus().getDescription() : "Unknown";
            if (isExecuted && !passed) {
                statusDescription = "Wrong Answer";
            }

            Long executionTimeMs = judge0Response.getTime() != null ? 
                    (long)(judge0Response.getTime() * 1000) : null;
            Integer memoryUsedKb = judge0Response.getMemory();

            RunCodeResponse.TestResultData testResult = RunCodeResponse.TestResultData.builder()
                    .passed(passed)
                    .input(testCase.getInput())
                    .expectedOutput(expectedOutput)
                    .actualOutput(actualOutput)
                    .executionTimeMs(executionTimeMs)
                    .memoryUsedKb(memoryUsedKb)
                    .statusDescription(statusDescription)
                    .build();
            testResults.add(testResult);

            // Aggregate metrics
            if (passed) {
                passedCount++;
            }

            if (executionTimeMs != null) {
                totalTimeMs += executionTimeMs;
                timeRecordCount++;
            }

            if (memoryUsedKb != null) {
                totalMemoryKb += memoryUsedKb;
                memoryRecordCount++;
            }

            // Capture stdout/stderr from first result
            if (i == 0) {
                stdout = judge0Response.getStdout();
                stderr = judge0Response.getStderr();
            }
        }

        // Calculate averages
        Double avgExecutionTimeMs = timeRecordCount > 0 ? (totalTimeMs / timeRecordCount) : null;
        Integer avgMemoryUsedKb = memoryRecordCount > 0 ? (totalMemoryKb / memoryRecordCount) : null;

        return RunCodeResponse.builder()
                .testResults(testResults)
                .allTestsPassed(passedCount == results.size())
                .testsPassed(passedCount)
                .testsTotal(results.size())
                .stdout(stdout)
                .stderr(stderr)
                .avgExecutionTimeMs(avgExecutionTimeMs)
                .avgMemoryUsedKb(avgMemoryUsedKb)
                .build();
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
     * Internal record to pair a test case with its Judge0 execution result.
     */
    private record TestExecutionResult(
            CodingTaskContent.TestCase testCase,
            Judge0SubmissionResponse judge0Response
    ) {}
}
