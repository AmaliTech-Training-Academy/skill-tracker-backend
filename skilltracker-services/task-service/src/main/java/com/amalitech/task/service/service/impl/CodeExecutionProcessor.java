package com.amalitech.task.service.service.impl;

import com.amalitech.task.service.dto.client.request.Judge0SubmissionRequest;
import com.amalitech.task.service.dto.client.response.Judge0SubmissionResponse;
import com.amalitech.task.service.dto.response.RunCodeResponse;
import com.amalitech.task.service.exception.ResourceNotFoundException;
import com.amalitech.task.service.model.Task;
import com.amalitech.task.service.model.content.impl.CodingTaskContent;
import com.amalitech.task.service.repository.TaskRepository;
import com.amalitech.task.service.service.CodeExecutionService;
import com.amalitech.task.service.service.Judge0Client;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.UUID;

/**
 * Service responsible for executing user-submitted code against test cases
 * using the Judge0 API.
 * 
 * Orchestrates the complete code execution workflow:
 * <ul>
 * <li>Retrieves the task and validates it is a coding task</li>
 * <li>Executes all associated test cases in parallel (up to 5 concurrent
 * executions)</li>
 * <li>Compares actual output against expected output with normalized whitespace
 * handling</li>
 * <li>Aggregates execution statistics and builds comprehensive test
 * results</li>
 * <li>Provides fallback error handling when Judge0 is unavailable</li>
 * </ul>
 * 
 * The service uses reactive programming (Project Reactor) for non-blocking I/O
 * operations
 * and elastic scheduling for blocking database calls.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CodeExecutionProcessor implements CodeExecutionService {

    private final Judge0Client judge0Client;
    private final TaskRepository taskRepository;

    private static final int JUDGE0_STATUS_ACCEPTED = 3;
    private static final int MAX_CONCURRENCY = 5;

    /**
     * Executes user-submitted code against all test cases for a specified coding
     * task.
     * 
     * Performs the following sequence:
     * <ol>
     * <li>Retrieves the task by ID from the database</li>
     * <li>Validates the task is a coding task with test cases defined</li>
     * <li>Submits code to Judge0 for each test case in parallel</li>
     * <li>Compares actual vs expected output with normalized whitespace</li>
     * <li>Aggregates and returns comprehensive execution statistics</li>
     * </ol>
     * 
     * @param taskId     the unique identifier of the coding task
     * @param code       the user-submitted source code to execute
     * @param languageId the programming language ID (Judge0 language identifier)
     * @return a Mono containing the aggregated test results and execution
     *         statistics
     * @throws IllegalArgumentException if task is not found or is not a coding task
     */
    @Override
    public Mono<RunCodeResponse> executeCode(UUID taskId, String code, Integer languageId) {
        return Mono.fromCallable(() -> taskRepository.findById(taskId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(taskOptional -> taskOptional
                        .map(Mono::just)
                        .orElseGet(() -> Mono.error(new ResourceNotFoundException(
                                "Task not found: " + taskId))))
                .flatMap(task -> executeTestCasesParallel(task, code, languageId));
    }

    /**
     * Executes all test cases for a coding task in parallel using a bounded elastic
     * scheduler.
     * 
     * Process:
     * <ol>
     * <li>Extracts and validates test cases from the coding task</li>
     * <li>Creates a Judge0 submission request for each test case with provided
     * code</li>
     * <li>Submits requests in parallel with a concurrency limit of 5</li>
     * <li>Captures both success and failure results (errors become system error
     * responses)</li>
     * <li>Collects all results and builds comprehensive response with aggregated
     * stats</li>
     * </ol>
     * 
     * @param task       the task containing coding task content and test cases
     * @param code       the source code to execute
     * @param languageId the programming language identifier for Judge0
     * @return a Mono containing aggregated test execution results and statistics
     * @throws IllegalArgumentException if task is not a coding task or has no test
     *                                  cases
     */
    private Mono<RunCodeResponse> executeTestCasesParallel(Task task, String code, Integer languageId) {
        if (!(task.getContent() instanceof CodingTaskContent content)) {
            return Mono.error(new IllegalArgumentException("Task is not a coding task"));
        }

        String harness = content.getSubmissionHarness();
        if (harness == null || harness.isBlank()) {
            return Mono.error(new IllegalStateException("Task execution harness is missing"));
        }

        List<CodingTaskContent.TestCase> testCases = content.getTestCases();
        if (testCases == null || testCases.isEmpty()) {
            return Mono.error(new IllegalArgumentException("Task has no test cases"));
        }

        String finalExecutableCode = code + "\n\n" + harness;

        log.debug("Executing code for task {}. User code lines: {}, Harness lines: {}",
                task.getId(),
                code.split("\n", -1).length,
                harness.split("\n", -1).length);

        return Flux.fromIterable(testCases)
                .flatMapSequential(testCase -> {
                    Judge0SubmissionRequest request = Judge0SubmissionRequest.builder()
                            .languageId(languageId)
                            .sourceCode(finalExecutableCode)
                            .stdin(testCase.getInput())
                            .expectedOutput(null)
                            .build();

                    return judge0Client.executeSubmission(request)
                            .map(result -> new TestExecutionResult(testCase, result))
                            .onErrorResume(e -> {
                                log.error("Judge0 execution failed for test case. Input: {}", testCase.getInput(), e);
                                return Mono.just(new TestExecutionResult(
                                        testCase,
                                        createSystemErrorResponse(e)));
                            });
                }, MAX_CONCURRENCY)
                .collectList()
                .map(this::buildResponse);
    }

    /**
     * Aggregates individual test execution results into a comprehensive response.
     * 
     * Transforms raw Judge0 results into a unified response by:
     * <ul>
     * <li>Processing each test result to determine pass/fail status and
     * metrics</li>
     * <li>Accumulating execution statistics (time, memory, pass count)</li>
     * <li>Calculating averages across all test executions</li>
     * <li>Building a complete RunCodeResponse with individual test details and
     * aggregates</li>
     * </ul>
     * 
     * @param results list of individual test execution results from Judge0
     * @return a comprehensive RunCodeResponse with test results and aggregated
     *         statistics
     */
    private RunCodeResponse buildResponse(List<TestExecutionResult> results) {
        var stats = new ExecutionStats();
        List<RunCodeResponse.TestResultData> testResults = results.stream()
                .map(result -> processSingleResult(result, stats))
                .toList();

        return RunCodeResponse.builder()
                .testResults(testResults)
                .allTestsPassed(stats.passedCount == results.size())
                .testsPassed(stats.passedCount)
                .testsTotal(results.size())
                .stdout(stats.firstStdout)
                .stderr(stats.firstStderr)
                .avgExecutionTimeMs(stats.calculateAvgTime())
                .avgMemoryUsedKb(stats.calculateAvgMemory())
                .build();
    }

    /**
     * Processes a single test execution result from Judge0.
     * 
     * Evaluates the test by:
     * <ul>
     * <li>Normalizing both expected and actual output (trims whitespace,
     * standardizes line endings)</li>
     * <li>Checking if Judge0 status indicates successful execution (status ID
     * 3)</li>
     * <li>Comparing normalized outputs for correctness</li>
     * <li>Setting appropriate status description (including "Wrong Answer" for
     * mismatches)</li>
     * <li>Extracting execution metrics (time in milliseconds, memory in KB)</li>
     * <li>Accumulating statistics for later aggregation</li>
     * </ul>
     * 
     * @param result the individual test execution result containing test case and
     *               Judge0 response
     * @param stats  accumulator object to track aggregated metrics across all tests
     * @return structured test result data with pass/fail status and execution
     *         details
     */
    private RunCodeResponse.TestResultData processSingleResult(TestExecutionResult result, ExecutionStats stats) {
        Judge0SubmissionResponse response = result.judge0Response();
        CodingTaskContent.TestCase testCase = result.testCase();

        String expectedNormalized = normalize(testCase.getExpectedOutput());
        String actualRaw = response.getStdout() != null ? response.getStdout() : "";
        String actualNormalized = normalize(actualRaw);

        boolean isAcceptedStatus = response.getStatus() != null
                && response.getStatus().getId() == JUDGE0_STATUS_ACCEPTED;
        boolean passed = isAcceptedStatus && expectedNormalized.equals(actualNormalized);

        String statusDesc = (response.getStatus() != null) ? response.getStatus().getDescription() : "Unknown";
        if (isAcceptedStatus && !passed) {
            statusDesc = "Wrong Answer";
        }

        Long timeMs = response.getTime() != null ? (long) (response.getTime() * 1000) : null;
        Integer memoryKb = response.getMemory();

        stats.accumulate(passed, timeMs, memoryKb, response.getStdout(), response.getStderr());

        return RunCodeResponse.TestResultData.builder()
                .passed(passed)
                .input(testCase.getInput())
                .expectedOutput(expectedNormalized)
                .actualOutput(actualNormalized)
                .executionTimeMs(timeMs)
                .memoryUsedKb(memoryKb)
                .statusDescription(statusDesc)
                .build();
    }

    /**
     * Normalizes output strings for consistent comparison.
     * 
     * Handles varying line ending formats and surrounding whitespace by:
     * <ul>
     * <li>Trimming leading and trailing whitespace</li>
     * <li>Converting Windows-style CRLF line endings to Unix-style LF</li>
     * <li>Converting old Mac-style CR line endings to Unix-style LF</li>
     * <li>Preserving the semantic content of multi-line output</li>
     * </ul>
     * 
     * @param s the output string to normalize (may be null)
     * @return normalized string, or empty string if input is null
     */
    private String normalize(String s) {
        if (s == null)
            return "";
        return s.trim().replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n");
    }

    /**
     * Accumulator for aggregated execution statistics across all test cases.
     * 
     * Tracks:
     * <ul>
     * <li>Count of passed tests</li>
     * <li>Total and average execution time across tests</li>
     * <li>Total and average memory usage across tests</li>
     * <li>First stdout and stderr outputs captured (for display)</li>
     * </ul>
     */
    private static class ExecutionStats {
        int passedCount = 0;
        double totalTimeMs = 0;
        int totalMemoryKb = 0;
        int timeRecordCount = 0;
        int memoryRecordCount = 0;
        String firstStdout = null;
        String firstStderr = null;

        /**
         * Accumulates execution metrics for a single test case into aggregate
         * statistics.
         * 
         * Updates pass count, total execution time and memory, and captures first
         * outputs.
         * 
         * @param passed   whether the test case passed
         * @param timeMs   execution time in milliseconds (nullable)
         * @param memoryKb memory used in kilobytes (nullable)
         * @param stdout   standard output from execution
         * @param stderr   standard error from execution
         */
        void accumulate(boolean passed, Long timeMs, Integer memoryKb, String stdout, String stderr) {
            if (passed)
                passedCount++;
            if (timeMs != null) {
                totalTimeMs += timeMs;
                timeRecordCount++;
            }
            if (memoryKb != null) {
                totalMemoryKb += memoryKb;
                memoryRecordCount++;
            }
            if (firstStdout == null)
                firstStdout = stdout;
            if (firstStderr == null)
                firstStderr = stderr;
        }

        /**
         * Calculates average execution time across all test cases.
         * 
         * @return average execution time in milliseconds, or null if no time metrics
         *         were recorded
         */
        Double calculateAvgTime() {
            return timeRecordCount > 0 ? totalTimeMs / timeRecordCount : null;
        }

        /**
         * Calculates average memory usage across all test cases.
         * 
         * @return average memory used in kilobytes, or null if no memory metrics were
         *         recorded
         */
        Integer calculateAvgMemory() {
            return memoryRecordCount > 0 ? totalMemoryKb / memoryRecordCount : null;
        }
    }

    /**
     * Creates a fallback error response when Judge0 submission fails.
     * 
     * Generates a valid Judge0SubmissionResponse object with system error status
     * when:
     * <ul>
     * <li>Judge0 service is unreachable</li>
     * <li>Network connectivity issues occur</li>
     * <li>Judge0 returns an error response</li>
     * </ul>
     * 
     * This prevents exceptions from being silently swallowed and ensures the user
     * receives an explicit failure notification with the underlying error message.
     * 
     * @param e the exception that caused the submission to fail
     * @return a valid Judge0SubmissionResponse indicating system error with error
     *         message in stderr
     */
    private Judge0SubmissionResponse createSystemErrorResponse(Throwable e) {
        return Judge0SubmissionResponse.builder()
                .status(Judge0SubmissionResponse.Judge0Status.builder()
                        .id(-1)
                        .description("System Error: Execution Failed")
                        .build())
                .stdout("")
                .stderr("Execution service unavailable: " + e.getMessage())
                .time(0.0)
                .memory(0)
                .build();
    }

    /**
     * Record pairing a test case with its Judge0 execution result.
     *
     * Used internally to track both the input/expected output and the actual Judge0
     * response.
     */
    private record TestExecutionResult(CodingTaskContent.TestCase testCase, Judge0SubmissionResponse judge0Response) {
    }
}