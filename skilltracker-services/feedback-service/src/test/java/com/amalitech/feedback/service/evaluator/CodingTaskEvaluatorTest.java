package com.amalitech.feedback.service.evaluator;

import com.amalitech.common.event.events.SubmissionCreatedEvent;
import com.amalitech.common.event.events.SubmissionEvaluatedEvent;
import com.amalitech.common.event.events.SubmissionExecutedEvent;
import com.amalitech.feedback.service.config.RabbitMQConfig;
import com.amalitech.feedback.service.dto.client.TaskDTO;
import com.amalitech.feedback.service.dto.client.response.Judge0SubmissionResponse;
import com.amalitech.feedback.service.dto.client.submission.DetailedEvaluationResponse;
import com.amalitech.feedback.service.exception.InvalidTaskException;
import com.amalitech.feedback.service.service.AIFeedbackClient;
import com.amalitech.feedback.service.service.Judge0Client;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CodingTaskEvaluator Tests")
class CodingTaskEvaluatorTest {

    @Mock
    private Judge0Client judge0Client;

    @Mock
    private AIFeedbackClient aiFeedbackClient;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private CodingTaskEvaluator evaluator;

    private UUID submissionId;
    private UUID userId;
    private UUID taskId;

    @BeforeEach
    void setUp() {
        submissionId = UUID.randomUUID();
        userId = UUID.randomUUID();
        taskId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("getTaskType Tests")
    class GetTaskTypeTests {

        @Test
        @DisplayName("Should return 'CODING' as task type")
        void shouldReturnCodingTaskType() {
            assertEquals("CODING", evaluator.getTaskType());
        }
    }

    @Nested
    @DisplayName("evaluate() Tests")
    class EvaluateTests {

        @Test
        @DisplayName("Should evaluate submission successfully with AI feedback")
        void shouldEvaluateSuccessfullyWithAIFeedback() throws InterruptedException, JsonProcessingException {
            SubmissionCreatedEvent event = createSubmissionEvent(2, true);
            List<Judge0SubmissionResponse> judge0Results = createPassingResults(2);
            DetailedEvaluationResponse aiFeedback = createAIFeedback(85, "Well-written code");

            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(judge0Client.executeSubmission(any()))
                    .thenReturn(Mono.just(judge0Results.get(0)))
                    .thenReturn(Mono.just(judge0Results.get(1)));
            when(aiFeedbackClient.generateDetailedFeedback(any(TaskDTO.class), anyString(), any()))
                    .thenReturn(Mono.just(aiFeedback));

            SubmissionEvaluatedEvent result = blockAndGetResult(evaluator.evaluate(event));

            assertNotNull(result);
            assertEquals(submissionId, result.getSubmissionId());
            assertEquals(userId, result.getUserId());
            assertEquals(85, result.getScore());
            assertTrue(result.isCorrect());
            assertEquals("COMPLETED", result.getStatus());
            assertEquals("Well-written code", result.getOverallFeedback());
            assertNotNull(result.getTestResults());
            assertEquals(2, result.getTestResults().size());

            verify(judge0Client, times(2)).executeSubmission(any());
            verify(aiFeedbackClient, times(1)).generateDetailedFeedback(any(TaskDTO.class), anyString(), any());
            verify(rabbitTemplate, times(1)).convertAndSend(
                    eq(RabbitMQConfig.SUBMISSION_EXCHANGE),
                    eq(RabbitMQConfig.SUBMISSION_EXECUTED_ROUTING_KEY),
                    any(SubmissionExecutedEvent.class)
            );
        }

        @Test
        @DisplayName("Should fall back to basic score when AI feedback fails")
        void shouldFallbackWhenAIFeedbackFails() throws InterruptedException {
            SubmissionCreatedEvent event = createSubmissionEvent(2, true);
            List<Judge0SubmissionResponse> judge0Results = createPassingResults(2);

            when(judge0Client.executeSubmission(any()))
                    .thenReturn(Mono.just(judge0Results.get(0)))
                    .thenReturn(Mono.just(judge0Results.get(1)));
            when(aiFeedbackClient.generateDetailedFeedback(any(TaskDTO.class), anyString(), any()))
                    .thenReturn(Mono.error(new RuntimeException("AI service unavailable")));

            SubmissionEvaluatedEvent result = blockAndGetResult(evaluator.evaluate(event));

            assertEquals(100, result.getScore());
            assertTrue(result.isCorrect());
            assertNull(result.getDetailedFeedback());
            assertNotNull(result.getOverallFeedback());
            assertTrue(result.getOverallFeedback().contains("unavailable"));

            verify(rabbitTemplate, times(1)).convertAndSend(
                    eq(RabbitMQConfig.SUBMISSION_EXCHANGE),
                    eq(RabbitMQConfig.SUBMISSION_EXECUTED_ROUTING_KEY),
                    any(SubmissionExecutedEvent.class)
            );
        }

        @Test
        @DisplayName("Should fail when submission has no test cases")
        void shouldFailWhenNoTestCases() throws InterruptedException {
            SubmissionCreatedEvent event = createSubmissionEvent(0, false);

            assertThrows(InvalidTaskException.class, () -> blockAndGetResult(evaluator.evaluate(event)));

            verify(judge0Client, never()).executeSubmission(any());
            verify(aiFeedbackClient, never()).generateDetailedFeedback(any(), any(), any());
        }

        @Test
        @DisplayName("Should calculate correct pass/fail based on test results")
        void shouldCalculatePassFailCorrectly() throws InterruptedException, JsonProcessingException {
            SubmissionCreatedEvent event = createSubmissionEvent(3, true);
            List<Judge0SubmissionResponse> mixedResults = List.of(
                    createPassingResult(),
                    createFailingResult(),
                    createPassingResult()
            );
            DetailedEvaluationResponse aiFeedback = createAIFeedback(67, "Partial solution");

            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(judge0Client.executeSubmission(any()))
                    .thenReturn(Mono.just(mixedResults.get(0)))
                    .thenReturn(Mono.just(mixedResults.get(1)))
                    .thenReturn(Mono.just(mixedResults.get(2)));
            when(aiFeedbackClient.generateDetailedFeedback(any(TaskDTO.class), anyString(), any()))
                    .thenReturn(Mono.just(aiFeedback));

            SubmissionEvaluatedEvent result = blockAndGetResult(evaluator.evaluate(event));

            assertEquals(67, result.getScore());
            assertFalse(result.isCorrect());
            assertEquals(3, result.getTestResults().size());
            assertTrue(result.getTestResults().get(0).isPassed());
            assertFalse(result.getTestResults().get(1).isPassed());
            assertTrue(result.getTestResults().get(2).isPassed());
        }

        @Test
        @DisplayName("Should publish SubmissionExecutedEvent before AI feedback")
        void shouldPublishExecutionResultsBeforeAIFeedback() throws InterruptedException, JsonProcessingException {
            SubmissionCreatedEvent event = createSubmissionEvent(1, true);
            List<Judge0SubmissionResponse> results = createPassingResults(1);
            DetailedEvaluationResponse aiFeedback = createAIFeedback(90, "Excellent");

            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(judge0Client.executeSubmission(any())).thenReturn(Mono.just(results.get(0)));
            when(aiFeedbackClient.generateDetailedFeedback(any(TaskDTO.class), anyString(), any()))
                    .thenReturn(Mono.just(aiFeedback));

            SubmissionEvaluatedEvent result = blockAndGetResult(evaluator.evaluate(event));
            assertEquals(90, result.getScore());

            ArgumentCaptor<SubmissionExecutedEvent> executedCaptor = ArgumentCaptor.forClass(SubmissionExecutedEvent.class);
            verify(rabbitTemplate).convertAndSend(
                    eq(RabbitMQConfig.SUBMISSION_EXCHANGE),
                    eq(RabbitMQConfig.SUBMISSION_EXECUTED_ROUTING_KEY),
                    executedCaptor.capture()
            );

            SubmissionExecutedEvent executedEvent = executedCaptor.getValue();
            assertEquals(submissionId, executedEvent.getSubmissionId());
            assertTrue(executedEvent.isAllTestsPassed());
            assertEquals(1, executedEvent.getTestsTotal());
        }
    }

    @Nested
    @DisplayName("Score Extraction Tests")
    class ScoreExtractionTests {

        @Test
        @DisplayName("Should extract score correctly from AI feedback")
        void shouldExtractScoreFromAIFeedback() throws InterruptedException, JsonProcessingException {
            SubmissionCreatedEvent event = createSubmissionEvent(1, true);
            List<Judge0SubmissionResponse> results = createPassingResults(1);
            DetailedEvaluationResponse aiFeedback = createAIFeedback(75, "Good solution");

            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(judge0Client.executeSubmission(any())).thenReturn(Mono.just(results.get(0)));
            when(aiFeedbackClient.generateDetailedFeedback(any(TaskDTO.class), anyString(), any()))
                    .thenReturn(Mono.just(aiFeedback));

            SubmissionEvaluatedEvent result = blockAndGetResult(evaluator.evaluate(event));

            assertEquals(75, result.getScore());
            assertTrue(result.isCorrect());
        }

        @Test
        @DisplayName("Should mark as incorrect when score below 70")
        void shouldMarkIncorrectBelowThreshold() throws InterruptedException, JsonProcessingException {
            SubmissionCreatedEvent event = createSubmissionEvent(1, true);
            List<Judge0SubmissionResponse> results = createPassingResults(1);
            DetailedEvaluationResponse aiFeedback = createAIFeedback(65, "Needs improvement");

            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(judge0Client.executeSubmission(any())).thenReturn(Mono.just(results.get(0)));
            when(aiFeedbackClient.generateDetailedFeedback(any(TaskDTO.class), anyString(), any()))
                    .thenReturn(Mono.just(aiFeedback));

            SubmissionEvaluatedEvent result = blockAndGetResult(evaluator.evaluate(event));

            assertEquals(65, result.getScore());
            assertFalse(result.isCorrect());
        }

        @Test
        @DisplayName("Should handle null AI feedback gracefully")
        void shouldHandleNullAIFeedback() throws InterruptedException {
            SubmissionCreatedEvent event = createSubmissionEvent(2, true);
            List<Judge0SubmissionResponse> results = createPassingResults(2);

            when(judge0Client.executeSubmission(any()))
                    .thenReturn(Mono.just(results.get(0)))
                    .thenReturn(Mono.just(results.get(1)));
            when(aiFeedbackClient.generateDetailedFeedback(any(TaskDTO.class), anyString(), any()))
                    .thenReturn(Mono.error(new RuntimeException("Feedback generation failed")));

            SubmissionEvaluatedEvent result = blockAndGetResult(evaluator.evaluate(event));

            assertEquals(100, result.getScore());
            assertTrue(result.isCorrect());
        }
    }

    @Nested
    @DisplayName("Test Result Processing Tests")
    class TestResultProcessingTests {

        @Test
        @DisplayName("Should normalize output correctly for comparison")
        void shouldNormalizeOutputCorrectly() throws InterruptedException, JsonProcessingException {
            SubmissionCreatedEvent event = createSubmissionEvent(1, true);
            Judge0SubmissionResponse result = new Judge0SubmissionResponse();
            result.setStdout("hello\nworld\r\n");
            Judge0SubmissionResponse.Judge0Status status = new Judge0SubmissionResponse.Judge0Status();
            status.setId(3);
            status.setDescription("Accepted");
            result.setStatus(status);
            result.setTime(0.5);
            result.setMemory(128);
            DetailedEvaluationResponse aiFeedback = createAIFeedback(100, "Perfect");

            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(judge0Client.executeSubmission(any())).thenReturn(Mono.just(result));
            when(aiFeedbackClient.generateDetailedFeedback(any(TaskDTO.class), anyString(), any()))
                    .thenReturn(Mono.just(aiFeedback));

            SubmissionEvaluatedEvent result1 = blockAndGetResult(evaluator.evaluate(event));

            assertEquals(1, result1.getTestResults().size());
            assertEquals("helloworld", result1.getTestResults().get(0).getActualOutput());
        }

        @Test
        @DisplayName("Should calculate execution metrics correctly")
        void shouldCalculateExecutionMetricsCorrectly() throws InterruptedException, JsonProcessingException {
            SubmissionCreatedEvent event = createSubmissionEvent(3, true);
            List<Judge0SubmissionResponse> results = List.of(
                    createResultWithMetrics(0.5, 128),
                    createResultWithMetrics(0.8, 256),
                    createResultWithMetrics(0.6, 192)
            );
            DetailedEvaluationResponse aiFeedback = createAIFeedback(90, "Good");

            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(judge0Client.executeSubmission(any()))
                    .thenReturn(Mono.just(results.get(0)))
                    .thenReturn(Mono.just(results.get(1)))
                    .thenReturn(Mono.just(results.get(2)));
            when(aiFeedbackClient.generateDetailedFeedback(any(TaskDTO.class), anyString(), any()))
                    .thenReturn(Mono.just(aiFeedback));

            SubmissionEvaluatedEvent result = blockAndGetResult(evaluator.evaluate(event));

            assertEquals(633.0, result.getAvgExecutionTimeMs(), 1.0);
            assertEquals(192, result.getAvgMemoryUsedKb());
        }

        @Test
        @DisplayName("Should handle missing execution metrics")
        void shouldHandleMissingMetrics() throws InterruptedException, JsonProcessingException {
            SubmissionCreatedEvent event = createSubmissionEvent(2, true);
            
            Judge0SubmissionResponse resp1 = new Judge0SubmissionResponse();
            resp1.setStdout("output1");
            Judge0SubmissionResponse.Judge0Status status1 = new Judge0SubmissionResponse.Judge0Status();
            status1.setId(3);
            status1.setDescription("Accepted");
            resp1.setStatus(status1);
            resp1.setTime(null);
            resp1.setMemory(null);
            
            Judge0SubmissionResponse resp2 = new Judge0SubmissionResponse();
            resp2.setStdout("output2");
            Judge0SubmissionResponse.Judge0Status status2 = new Judge0SubmissionResponse.Judge0Status();
            status2.setId(3);
            status2.setDescription("Accepted");
            resp2.setStatus(status2);
            resp2.setTime(null);
            resp2.setMemory(null);
            
            List<Judge0SubmissionResponse> results = List.of(resp1, resp2);
            DetailedEvaluationResponse aiFeedback = createAIFeedback(100, "Perfect");

            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(judge0Client.executeSubmission(any()))
                    .thenReturn(Mono.just(results.get(0)))
                    .thenReturn(Mono.just(results.get(1)));
            when(aiFeedbackClient.generateDetailedFeedback(any(TaskDTO.class), anyString(), any()))
                    .thenReturn(Mono.just(aiFeedback));

            SubmissionEvaluatedEvent result = blockAndGetResult(evaluator.evaluate(event));

            assertEquals(0.0, result.getAvgExecutionTimeMs());
            assertEquals(0, result.getAvgMemoryUsedKb());
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle Judge0 execution error")
        void shouldHandleJudge0Error() {
            SubmissionCreatedEvent event = createSubmissionEvent(1, true);

            when(judge0Client.executeSubmission(any()))
                    .thenReturn(Mono.error(new RuntimeException("Judge0 API error")));

            assertThrows(RuntimeException.class, () -> blockAndGetResult(evaluator.evaluate(event)));
        }

        @Test
        @DisplayName("Should recover from AI feedback error with fallback")
        void shouldRecoverFromAIError() throws InterruptedException {
            SubmissionCreatedEvent event = createSubmissionEvent(3, true);
            List<Judge0SubmissionResponse> results = List.of(
                    createPassingResult(),
                    createPassingResult(),
                    createFailingResult()
            );

            doReturn("{}").when(objectMapper).writeValueAsString(any());
            when(judge0Client.executeSubmission(any()))
                    .thenReturn(Mono.just(results.get(0)))
                    .thenReturn(Mono.just(results.get(1)))
                    .thenReturn(Mono.just(results.get(2)));
            when(aiFeedbackClient.generateDetailedFeedback(any(TaskDTO.class), anyString(), any()))
                    .thenReturn(Mono.error(new RuntimeException("AI service error")));

            SubmissionEvaluatedEvent result = blockAndGetResult(evaluator.evaluate(event));

            assertEquals(66, result.getScore());
            assertFalse(result.isCorrect());
            assertNull(result.getDetailedFeedback());
        }

        @Test
        @DisplayName("Should handle malformed AI feedback")
        void shouldHandleMalformedAIFeedback() throws InterruptedException {
            SubmissionCreatedEvent event = createSubmissionEvent(1, true);
            List<Judge0SubmissionResponse> results = createPassingResults(1);

            DetailedEvaluationResponse malformedFeedback = new DetailedEvaluationResponse();
            malformedFeedback.setEvaluation(null);

            doReturn("{}").when(objectMapper).writeValueAsString(any());
            when(judge0Client.executeSubmission(any())).thenReturn(Mono.just(results.get(0)));
            when(aiFeedbackClient.generateDetailedFeedback(any(TaskDTO.class), anyString(), any()))
                    .thenReturn(Mono.just(malformedFeedback));

            SubmissionEvaluatedEvent result = blockAndGetResult(evaluator.evaluate(event));

            assertEquals(100, result.getScore());
            assertTrue(result.isCorrect());
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle single test case")
        void shouldHandleSingleTestCase() throws InterruptedException, JsonProcessingException {
            SubmissionCreatedEvent event = createSubmissionEvent(1, true);
            List<Judge0SubmissionResponse> results = createPassingResults(1);
            DetailedEvaluationResponse aiFeedback = createAIFeedback(100, "Perfect");

            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(judge0Client.executeSubmission(any())).thenReturn(Mono.just(results.get(0)));
            when(aiFeedbackClient.generateDetailedFeedback(any(TaskDTO.class), anyString(), any()))
                    .thenReturn(Mono.just(aiFeedback));

            SubmissionEvaluatedEvent result = blockAndGetResult(evaluator.evaluate(event));

            assertEquals(1, result.getTestResults().size());
            assertTrue(result.getTestResults().get(0).isPassed());
        }

        @Test
        @DisplayName("Should handle large number of test cases")
        void shouldHandleLargeNumberOfTests() throws InterruptedException, JsonProcessingException {
            SubmissionCreatedEvent event = createSubmissionEvent(50, true);
            List<Judge0SubmissionResponse> results = createPassingResults(50);
            DetailedEvaluationResponse aiFeedback = createAIFeedback(100, "Excellent");

            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(judge0Client.executeSubmission(any())).thenAnswer(inv -> Mono.just(results.get(0)));
            when(aiFeedbackClient.generateDetailedFeedback(any(TaskDTO.class), anyString(), any()))
                    .thenReturn(Mono.just(aiFeedback));

            SubmissionEvaluatedEvent result = blockAndGetResult(evaluator.evaluate(event));

            assertEquals(50, result.getTestResults().size());
            assertEquals(100, result.getScore());
        }

        @Test
        @DisplayName("Should handle all tests failing")
        void shouldHandleAllTestsFailing() throws InterruptedException, JsonProcessingException {
            SubmissionCreatedEvent event = createSubmissionEvent(3, true);
            List<Judge0SubmissionResponse> results = List.of(
                    createFailingResult(),
                    createFailingResult(),
                    createFailingResult()
            );
            DetailedEvaluationResponse aiFeedback = createAIFeedback(10, "Major issues");

            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(judge0Client.executeSubmission(any()))
                    .thenReturn(Mono.just(results.get(0)))
                    .thenReturn(Mono.just(results.get(1)))
                    .thenReturn(Mono.just(results.get(2)));
            when(aiFeedbackClient.generateDetailedFeedback(any(TaskDTO.class), anyString(), any()))
                    .thenReturn(Mono.just(aiFeedback));

            SubmissionEvaluatedEvent result = blockAndGetResult(evaluator.evaluate(event));

            assertEquals(10, result.getScore());
            assertFalse(result.isCorrect());
            assertEquals(3, result.getTestResults().size());
            result.getTestResults().forEach(tr -> assertFalse(tr.isPassed()));
        }

        @Test
        @DisplayName("Should handle runtime error status from Judge0")
        void shouldHandleRuntimeError() throws InterruptedException, JsonProcessingException {
            SubmissionCreatedEvent event = createSubmissionEvent(1, true);
            Judge0SubmissionResponse errorResult = new Judge0SubmissionResponse();
            errorResult.setStdout(null);
            errorResult.setStderr("Runtime error occurred");
            Judge0SubmissionResponse.Judge0Status status = new Judge0SubmissionResponse.Judge0Status();
            status.setId(6);
            status.setDescription("Runtime Error");
            errorResult.setStatus(status);
            
            DetailedEvaluationResponse aiFeedback = createAIFeedback(0, "Runtime error");

            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(judge0Client.executeSubmission(any())).thenReturn(Mono.just(errorResult));
            when(aiFeedbackClient.generateDetailedFeedback(any(TaskDTO.class), anyString(), any()))
                    .thenReturn(Mono.just(aiFeedback));

            SubmissionEvaluatedEvent result = blockAndGetResult(evaluator.evaluate(event));

            assertEquals(0, result.getScore());
            assertFalse(result.isCorrect());
            assertEquals("Runtime Error", result.getTestResults().get(0).getStatusDescription());
        }
    }

    // ==================== Helper Methods ====================

    private SubmissionCreatedEvent createSubmissionEvent(int testCaseCount, boolean hasCode) {
        List<SubmissionCreatedEvent.TestCaseData> testCases = createTestCases(testCaseCount);

        return SubmissionCreatedEvent.builder()
                .submissionId(submissionId)
                .userId(userId)
                .taskId(taskId)
                .contentToEvaluate(hasCode ? "def solution():\n    return 42" : "")
                .languageId(71)
                .testCases(testCases)
                .build();
    }

    private List<SubmissionCreatedEvent.TestCaseData> createTestCases(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> SubmissionCreatedEvent.TestCaseData.builder()
                        .input("input" + i)
                        .expectedOutput("output" + i)
                        .build())
                .toList();
    }

    private List<Judge0SubmissionResponse> createPassingResults(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> {
                    Judge0SubmissionResponse resp = new Judge0SubmissionResponse();
                    resp.setStdout("output" + i);
                    Judge0SubmissionResponse.Judge0Status status = new Judge0SubmissionResponse.Judge0Status();
                    status.setId(3);
                    status.setDescription("Accepted");
                    resp.setStatus(status);
                    resp.setTime(0.5);
                    resp.setMemory(128);
                    return resp;
                })
                .toList();
    }

    private Judge0SubmissionResponse createPassingResult() {
        Judge0SubmissionResponse resp = new Judge0SubmissionResponse();
        resp.setStdout("correct output");
        Judge0SubmissionResponse.Judge0Status status = new Judge0SubmissionResponse.Judge0Status();
        status.setId(3);
        status.setDescription("Accepted");
        resp.setStatus(status);
        resp.setTime(0.5);
        resp.setMemory(128);
        return resp;
    }

    private Judge0SubmissionResponse createFailingResult() {
        Judge0SubmissionResponse resp = new Judge0SubmissionResponse();
        resp.setStdout("wrong output");
        Judge0SubmissionResponse.Judge0Status status = new Judge0SubmissionResponse.Judge0Status();
        status.setId(4);
        status.setDescription("Wrong Answer");
        resp.setStatus(status);
        resp.setTime(0.5);
        resp.setMemory(128);
        return resp;
    }

    private Judge0SubmissionResponse createResultWithMetrics(double time, int memory) {
        Judge0SubmissionResponse resp = new Judge0SubmissionResponse();
        resp.setStdout("output");
        Judge0SubmissionResponse.Judge0Status status = new Judge0SubmissionResponse.Judge0Status();
        status.setId(3);
        status.setDescription("Accepted");
        resp.setStatus(status);
        resp.setTime(time);
        resp.setMemory(memory);
        return resp;
    }

    private DetailedEvaluationResponse createAIFeedback(int score, String summary) {
        DetailedEvaluationResponse feedback = new DetailedEvaluationResponse();
        DetailedEvaluationResponse.Evaluation evaluation = new DetailedEvaluationResponse.Evaluation();
        DetailedEvaluationResponse.OverallEvaluation overall = new DetailedEvaluationResponse.OverallEvaluation();
        
        overall.setPercentage(score);
        overall.setSummary(summary);
        evaluation.setOverall(overall);
        feedback.setEvaluation(evaluation);
        
        return feedback;
    }

    private <T> T blockAndGetResult(Mono<T> mono) throws InterruptedException {
        AtomicReference<T> resultHolder = new AtomicReference<>();
        AtomicReference<Throwable> errorHolder = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        mono.subscribe(
                result -> {
                    resultHolder.set(result);
                    latch.countDown();
                },
                error -> {
                    errorHolder.set(error);
                    latch.countDown();
                }
        );

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Operation did not complete in time");
        
        if (errorHolder.get() != null) {
            Throwable error = errorHolder.get();
            if (error instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException(error);
        }

        return resultHolder.get();
    }
}
