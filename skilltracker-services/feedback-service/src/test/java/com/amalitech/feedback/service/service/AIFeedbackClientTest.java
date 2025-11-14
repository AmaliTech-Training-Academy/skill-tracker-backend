package com.amalitech.feedback.service.service;

import com.amalitech.common.event.events.SubmissionCreatedEvent;
import com.amalitech.feedback.service.dto.client.TaskDTO;
import com.amalitech.feedback.service.dto.client.response.Judge0SubmissionResponse;
import com.amalitech.feedback.service.dto.client.submission.DetailedEvaluationResponse;
import com.amalitech.feedback.service.dto.client.submission.impl.CodingSubmissionFeedback;
import com.amalitech.feedback.service.dto.client.submission.impl.EssaySubmissionFeedback;
import com.amalitech.feedback.service.exception.AiResponseParsingException;
import com.amalitech.feedback.service.exception.InvalidAiResponseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AIFeedbackClientTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private PromptTemplate codingEvaluationPromptTemplate;

    @Mock
    private PromptTemplate simpleFeedbackPromptTemplate;

    @Mock
    private PromptTemplate writtenEvaluationPromptTemplate;

    @InjectMocks
    private AIFeedbackClient aiFeedbackClient;

    @Mock
    private ChatClient chatClient;

    private TaskDTO taskDTO;
    private SubmissionCreatedEvent submissionEvent;

    @BeforeEach
    void setUp() {
        taskDTO = createTaskDTO();
        submissionEvent = createSubmissionEvent();
    }

    @Nested
    class GenerateDetailedFeedbackTests {

        @Test
        void shouldReturnDetailedResponse_whenAiCallIsSuccessful() throws Exception {
            String userCode = "public class Main {}";
            List<Judge0SubmissionResponse> executionResults = Collections.emptyList();
            String fakeJsonResponse = createDetailedEvaluationJson();

            DetailedEvaluationResponse expectedResponse = createDetailedEvaluationResponse();

            setupChatClientMocks(fakeJsonResponse);
            when(objectMapper.readValue(any(String.class), (Class<DetailedEvaluationResponse>) any()))
                    .thenReturn(expectedResponse);

            Mono<DetailedEvaluationResponse> result = aiFeedbackClient.generateDetailedFeedback(
                    taskDTO, userCode, executionResults
            );

            StepVerifier.create(result)
                    .expectNext(expectedResponse)
                    .verifyComplete();

            verify(chatClientBuilder).build();
            verify(codingEvaluationPromptTemplate).create(any(Map.class));
        }

        @Test
        void shouldThrowAiResponseParsingException_whenAiReturnsInvalidJson() {
            String userCode = "public class Main {}";
            List<Judge0SubmissionResponse> executionResults = Collections.emptyList();
            String invalidJson = "invalid json";

            setupChatClientMocks(invalidJson);

            Mono<DetailedEvaluationResponse> result = aiFeedbackClient.generateDetailedFeedback(
                    taskDTO, userCode, executionResults
            );

            StepVerifier.create(result)
                    .expectError(AiResponseParsingException.class)
                    .verify();
        }

        @Test
        void shouldHandleEmptyJsonResponse_throwsInvalidAiResponseException() throws Exception {
            String userCode = "public class Main {}";
            List<Judge0SubmissionResponse> executionResults = Collections.emptyList();

            setupChatClientMocks("");
            when(objectMapper.readValue(any(String.class), (Class<DetailedEvaluationResponse>) any()))
                    .thenThrow(new InvalidAiResponseException("AI response was empty."));

            Mono<DetailedEvaluationResponse> result = aiFeedbackClient.generateDetailedFeedback(
                    taskDTO, userCode, executionResults
            );

            StepVerifier.create(result)
                    .expectError(InvalidAiResponseException.class)
                    .verify();
        }

        @Test
        void shouldParseJsonWithMarkdown_removeBackticksAndParse() throws Exception {
            String userCode = "int[] arr = {1, 2, 3};";
            List<Judge0SubmissionResponse> executionResults = Collections.emptyList();
            String jsonWithMarkdown = "```json\n" + createDetailedEvaluationJson() + "\n```";

            DetailedEvaluationResponse expectedResponse = createDetailedEvaluationResponse();

            setupChatClientMocks(jsonWithMarkdown);
            when(objectMapper.readValue(any(String.class), (Class<DetailedEvaluationResponse>) any()))
                    .thenReturn(expectedResponse);

            Mono<DetailedEvaluationResponse> result = aiFeedbackClient.generateDetailedFeedback(
                    taskDTO, userCode, executionResults
            );

            StepVerifier.create(result)
                    .expectNext(expectedResponse)
                    .verifyComplete();
        }

        @Test
        void shouldIncludeTaskDetailsInPromptVariables() throws Exception {
            String userCode = "public class Main {}";
            List<Judge0SubmissionResponse> executionResults = Collections.emptyList();
            String fakeJsonResponse = createDetailedEvaluationJson();
            DetailedEvaluationResponse expectedResponse = createDetailedEvaluationResponse();

            setupChatClientMocks(fakeJsonResponse);
            when(objectMapper.readValue(any(String.class), (Class<DetailedEvaluationResponse>) any()))
                    .thenReturn(expectedResponse);

            aiFeedbackClient.generateDetailedFeedback(taskDTO, userCode, executionResults)
                    .block();

            verify(codingEvaluationPromptTemplate).create(any(Map.class));
            verify(chatClientBuilder).build();
        }

        @Test
        void shouldUseDefaultValues_whenTaskFieldsAreNull() throws Exception {
            TaskDTO nullFieldsTask = new TaskDTO();
            nullFieldsTask.setId(UUID.randomUUID());

            String userCode = "public class Main {}";
            List<Judge0SubmissionResponse> executionResults = Collections.emptyList();
            String fakeJsonResponse = createDetailedEvaluationJson();
            DetailedEvaluationResponse expectedResponse = createDetailedEvaluationResponse();

            setupChatClientMocks(fakeJsonResponse);
            when(objectMapper.readValue(any(String.class), (Class<DetailedEvaluationResponse>) any()))
                    .thenReturn(expectedResponse);

            aiFeedbackClient.generateDetailedFeedback(nullFieldsTask, userCode, executionResults)
                    .block();

            verify(codingEvaluationPromptTemplate).create(any(Map.class));
        }
    }

    @Nested
    class GenerateCodingFeedbackTests {

        @Test
        void shouldGenerateCodingFeedback_usingDetailedFeedback() throws Exception {
            String userCode = "public class Main {}";
            List<Judge0SubmissionResponse> executionResults = Collections.emptyList();
            String fakeJsonResponse = createDetailedEvaluationJson();

            DetailedEvaluationResponse detailedResponse = createDetailedEvaluationResponse();

            setupChatClientMocks(fakeJsonResponse);
            when(objectMapper.readValue(any(String.class), (Class<DetailedEvaluationResponse>) any()))
                    .thenReturn(detailedResponse);

            Mono<CodingSubmissionFeedback> result = aiFeedbackClient.generateCodingFeedback(
                    taskDTO, userCode, executionResults
            );

            StepVerifier.create(result)
                    .assertNext(feedback -> {
                        assertNotNull(feedback);
                        assertEquals("Good", feedback.getCorrectnessFeedback());
                        assertEquals("Good", feedback.getEfficiencyFeedback());
                        assertEquals("Good", feedback.getStyleFeedback());
                        assertEquals("Excellent", feedback.getOverallSuggestion());
                    })
                    .verifyComplete();
        }

        @Test
        void shouldConvertDetailedResponseToSimpleFeedback() throws Exception {
            String userCode = "public class Main {}";
            List<Judge0SubmissionResponse> executionResults = Collections.emptyList();
            String fakeJsonResponse = createDetailedEvaluationJson();

            DetailedEvaluationResponse detailedResponse = createDetailedEvaluationResponse();

            setupChatClientMocks(fakeJsonResponse);
            when(objectMapper.readValue(any(String.class), (Class<DetailedEvaluationResponse>) any()))
                    .thenReturn(detailedResponse);

            Mono<CodingSubmissionFeedback> result = aiFeedbackClient.generateCodingFeedback(
                    taskDTO, userCode, executionResults
            );

            StepVerifier.create(result)
                    .assertNext(feedback -> {
                        assertEquals(1, feedback.getPassedTests());
                        assertEquals(1, feedback.getTotalTests());
                    })
                    .verifyComplete();
        }

        @Test
        void shouldFallbackToSimpleFeedback_whenDetailedFeedbackFails() throws Exception {
            String userCode = "public class Main {}";
            List<Judge0SubmissionResponse> executionResults = Collections.emptyList();

            when(chatClientBuilder.build()).thenThrow(new RuntimeException("Detailed feedback failed"));
            setupChatClientMocks("");

            String simpleFeedbackJson = createSimpleFeedbackJson();
            CodingSubmissionFeedback simpleFeedback = CodingSubmissionFeedback.builder()
                    .correctnessFeedback("Good")
                    .efficiencyFeedback("Good")
                    .styleFeedback("Good")
                    .overallSuggestion("Excellent")
                    .build();

            when(objectMapper.readValue(any(String.class), (Class<CodingSubmissionFeedback>) any()))
                    .thenReturn(simpleFeedback);

            Mono<CodingSubmissionFeedback> result = aiFeedbackClient.generateCodingFeedback(
                    taskDTO, userCode, executionResults
            );

            StepVerifier.create(result)
                    .assertNext(feedback -> assertNotNull(feedback))
                    .verifyComplete();
        }

        @Test
        void shouldGenerateCodingFeedback_fromSubmissionEvent() throws Exception {
            List<Judge0SubmissionResponse> executionResults = Collections.emptyList();
            String fakeJsonResponse = createDetailedEvaluationJson();

            DetailedEvaluationResponse detailedResponse = createDetailedEvaluationResponse();

            setupChatClientMocks(fakeJsonResponse);
            when(objectMapper.readValue(any(String.class), (Class<DetailedEvaluationResponse>) any()))
                    .thenReturn(detailedResponse);

            Mono<CodingSubmissionFeedback> result = aiFeedbackClient.generateCodingFeedback(
                    taskDTO, submissionEvent, executionResults
            );

            StepVerifier.create(result)
                    .assertNext(feedback -> assertNotNull(feedback))
                    .verifyComplete();
        }
    }

    @Nested
    class GenerateEssayFeedbackTests {

        @Test
        void shouldGenerateEssayFeedback_successfully() throws Exception {
            String essayJsonResponse = createEssayEvaluationJson();

            EssaySubmissionFeedback expectedFeedback = createEssaySubmissionFeedback();

            setupChatClientMocks(essayJsonResponse);
            when(objectMapper.readValue(any(String.class), (Class<EssaySubmissionFeedback>) any()))
                    .thenReturn(expectedFeedback);

            Mono<EssaySubmissionFeedback> result = aiFeedbackClient.generateEssayFeedback(
                    taskDTO, submissionEvent
            );

            StepVerifier.create(result)
                    .assertNext(feedback -> {
                        assertNotNull(feedback);
                        assertNotNull(feedback.getEvaluation());
                    })
                    .verifyComplete();

            verify(writtenEvaluationPromptTemplate).create(any(Map.class));
        }

        @Test
        void shouldThrowException_whenEssayJsonIsInvalid() {
            String invalidJson = "invalid json";

            setupChatClientMocks(invalidJson);

            Mono<EssaySubmissionFeedback> result = aiFeedbackClient.generateEssayFeedback(
                    taskDTO, submissionEvent
            );

            StepVerifier.create(result)
                    .expectError(AiResponseParsingException.class)
                    .verify();
        }

        @Test
        void shouldHandleEmptyEssayResponse() throws Exception {
            setupChatClientMocks("");
            when(objectMapper.readValue(any(String.class), (Class<EssaySubmissionFeedback>) any()))
                    .thenThrow(new InvalidAiResponseException("AI response was empty."));

            Mono<EssaySubmissionFeedback> result = aiFeedbackClient.generateEssayFeedback(
                    taskDTO, submissionEvent
            );

            StepVerifier.create(result)
                    .expectError(InvalidAiResponseException.class)
                    .verify();
        }

        @Test
        void shouldIncludeAllEssayDetailsInPromptVariables() throws Exception {
            String essayJsonResponse = createEssayEvaluationJson();
            EssaySubmissionFeedback expectedFeedback = createEssaySubmissionFeedback();

            setupChatClientMocks(essayJsonResponse);
            when(objectMapper.readValue(any(String.class), (Class<EssaySubmissionFeedback>) any()))
                    .thenReturn(expectedFeedback);

            aiFeedbackClient.generateEssayFeedback(taskDTO, submissionEvent).block();

            verify(writtenEvaluationPromptTemplate).create(any(Map.class));
        }

        @Test
        void shouldUseDefaultValues_whenEssayFieldsAreNull() throws Exception {
            SubmissionCreatedEvent nullFieldsEvent = SubmissionCreatedEvent.builder()
                    .contentToEvaluate("user essay")
                    .build();

            String essayJsonResponse = createEssayEvaluationJson();
            EssaySubmissionFeedback expectedFeedback = createEssaySubmissionFeedback();

            setupChatClientMocks(essayJsonResponse);
            when(objectMapper.readValue(any(String.class), (Class<EssaySubmissionFeedback>) any()))
                    .thenReturn(expectedFeedback);

            aiFeedbackClient.generateEssayFeedback(taskDTO, nullFieldsEvent).block();

            verify(writtenEvaluationPromptTemplate).create(any(Map.class));
        }

        @Test
        void shouldParseEssayJsonWithMarkdown() throws Exception {
            String essayJsonWithMarkdown = "```json\n" + createEssayEvaluationJson() + "\n```";
            EssaySubmissionFeedback expectedFeedback = createEssaySubmissionFeedback();

            setupChatClientMocks(essayJsonWithMarkdown);
            when(objectMapper.readValue(any(String.class), (Class<EssaySubmissionFeedback>) any()))
                    .thenReturn(expectedFeedback);

            Mono<EssaySubmissionFeedback> result = aiFeedbackClient.generateEssayFeedback(
                    taskDTO, submissionEvent
            );

            StepVerifier.create(result)
                    .assertNext(feedback -> assertNotNull(feedback))
                    .verifyComplete();
        }
    }

    @Nested
    class TestCaseFormattingTests {

        @Test
        void shouldFormatTestCases_withAllFields() throws Exception {
            Judge0SubmissionResponse testResult = createJudge0Response(
                    3, "Accepted", "Hello World", null, null
            );
            List<Judge0SubmissionResponse> executionResults = List.of(testResult);

            String userCode = "System.out.println(\"Hello World\");";
            String fakeJsonResponse = createDetailedEvaluationJson();
            DetailedEvaluationResponse expectedResponse = createDetailedEvaluationResponse();

            setupChatClientMocks(fakeJsonResponse);
            when(objectMapper.readValue(any(String.class), (Class<DetailedEvaluationResponse>) any()))
                    .thenReturn(expectedResponse);

            aiFeedbackClient.generateDetailedFeedback(taskDTO, userCode, executionResults)
                    .block();

            verify(codingEvaluationPromptTemplate).create(any(Map.class));
        }

        @Test
        void shouldHandleEmptyExecutionResults() throws Exception {
            String userCode = "public class Main {}";
            List<Judge0SubmissionResponse> executionResults = Collections.emptyList();
            String fakeJsonResponse = createDetailedEvaluationJson();
            DetailedEvaluationResponse expectedResponse = createDetailedEvaluationResponse();

            setupChatClientMocks(fakeJsonResponse);
            when(objectMapper.readValue(any(String.class), (Class<DetailedEvaluationResponse>) any()))
                    .thenReturn(expectedResponse);

            aiFeedbackClient.generateDetailedFeedback(taskDTO, userCode, executionResults)
                    .block();

            verify(codingEvaluationPromptTemplate).create(any(Map.class));
        }

        @Test
        void shouldHandleNullStatusInJudge0Response() throws Exception {
            Judge0SubmissionResponse testResult = new Judge0SubmissionResponse();
            testResult.setStdout("output");
            testResult.setStderr(null);
            testResult.setCompileOutput(null);
            List<Judge0SubmissionResponse> executionResults = List.of(testResult);

            String userCode = "code";
            String fakeJsonResponse = createDetailedEvaluationJson();
            DetailedEvaluationResponse expectedResponse = createDetailedEvaluationResponse();

            setupChatClientMocks(fakeJsonResponse);
            when(objectMapper.readValue(any(String.class), (Class<DetailedEvaluationResponse>) any()))
                    .thenReturn(expectedResponse);

            Mono<DetailedEvaluationResponse> result = aiFeedbackClient.generateDetailedFeedback(
                    taskDTO, userCode, executionResults
            );

            StepVerifier.create(result)
                    .expectNext(expectedResponse)
                    .verifyComplete();
        }

        @Test
        void shouldHandleMultipleTestCases() throws Exception {
            List<Judge0SubmissionResponse> executionResults = List.of(
                    createJudge0Response(3, "Accepted", "output1", null, null),
                    createJudge0Response(3, "Accepted", "output2", null, null),
                    createJudge0Response(4, "Wrong Answer", null, "error", null)
            );

            String userCode = "code";
            String fakeJsonResponse = createDetailedEvaluationJson();
            DetailedEvaluationResponse expectedResponse = createDetailedEvaluationResponse();

            setupChatClientMocks(fakeJsonResponse);
            when(objectMapper.readValue(any(String.class), (Class<DetailedEvaluationResponse>) any()))
                    .thenReturn(expectedResponse);

            Mono<DetailedEvaluationResponse> result = aiFeedbackClient.generateDetailedFeedback(
                    taskDTO, userCode, executionResults
            );

            StepVerifier.create(result)
                    .expectNext(expectedResponse)
                    .verifyComplete();
        }
    }

    @Nested
    class ErrorHandlingTests {

        @Test
        void shouldPropagateAiResponseParsingException() {
            String userCode = "code";
            List<Judge0SubmissionResponse> results = Collections.emptyList();

            setupChatClientMocks("invalid json");

            Mono<DetailedEvaluationResponse> result = aiFeedbackClient.generateDetailedFeedback(
                    taskDTO, userCode, results
            );

            StepVerifier.create(result)
                    .expectError(AiResponseParsingException.class)
                    .verify();
        }

        @Test
        void shouldHandleNullContentInResponse() throws Exception {
            String userCode = "code";
            List<Judge0SubmissionResponse> results = Collections.emptyList();

            setupChatClientMocks(null);

            Mono<DetailedEvaluationResponse> result = aiFeedbackClient.generateDetailedFeedback(
                    taskDTO, userCode, results
            );

            StepVerifier.create(result)
                    .expectError(InvalidAiResponseException.class)
                    .verify();
        }

        @Test
        void shouldHandleChatClientBuildFailure() {
            String userCode = "code";
            List<Judge0SubmissionResponse> results = Collections.emptyList();

            when(chatClientBuilder.build()).thenThrow(new RuntimeException("Build failed"));

            Mono<DetailedEvaluationResponse> result = aiFeedbackClient.generateDetailedFeedback(
                    taskDTO, userCode, results
            );

            StepVerifier.create(result)
                    .expectError(RuntimeException.class)
                    .verify();
        }
    }

    // ==================== Helper Methods ====================

    private void setupChatClientMocks(String responseContent) {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        
        var requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        when(chatClient.prompt(any(Prompt.class))).thenReturn(requestSpec);
        
        // Create a mock CallResponseSpec with content() method
        var callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        when(callResponseSpec.content()).thenReturn(responseContent);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        
        when(codingEvaluationPromptTemplate.create(any(Map.class))).thenReturn(new Prompt(""));
        when(simpleFeedbackPromptTemplate.create(any(Map.class))).thenReturn(new Prompt(""));
        when(writtenEvaluationPromptTemplate.create(any(Map.class))).thenReturn(new Prompt(""));
    }

    private TaskDTO createTaskDTO() {
        return TaskDTO.builder()
                .id(UUID.randomUUID())
                .skillName("Java")
                .description("Test Description")
                .difficulty(TaskDTO.TaskDifficulty.MEDIUM)
                .build();
    }

    private SubmissionCreatedEvent createSubmissionEvent() {
        return SubmissionCreatedEvent.builder()
                .submissionId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .taskId(UUID.randomUUID())
                .contentToEvaluate("code content")
                .skillName("Java")
                .difficulty("INTERMEDIATE")
                .taskTitle("Test Task")
                .taskDescription("Test Description")
                .detailedInstructions("Detailed instructions")
                .evaluationCriteria("Evaluation criteria")
                .rubric("Rubric")
                .build();
    }

    private DetailedEvaluationResponse createDetailedEvaluationResponse() {
        DetailedEvaluationResponse response = new DetailedEvaluationResponse();
        DetailedEvaluationResponse.Evaluation evaluation = new DetailedEvaluationResponse.Evaluation();

        DetailedEvaluationResponse.CorrectnessEvaluation correctness =
                new DetailedEvaluationResponse.CorrectnessEvaluation();
        correctness.setScore(10.0);
        correctness.setFeedback("Good");
        DetailedEvaluationResponse.TestResult testResult = new DetailedEvaluationResponse.TestResult();
        testResult.setPassed(true);
        correctness.setTestResults(List.of(testResult));

        DetailedEvaluationResponse.EfficiencyEvaluation efficiency =
                new DetailedEvaluationResponse.EfficiencyEvaluation();
        efficiency.setScore(10.0);
        efficiency.setFeedback("Good");

        DetailedEvaluationResponse.StyleEvaluation style =
                new DetailedEvaluationResponse.StyleEvaluation();
        style.setScore(10.0);
        style.setFeedback("Good");

        DetailedEvaluationResponse.OverallEvaluation overall =
                new DetailedEvaluationResponse.OverallEvaluation();
        overall.setTotalScore(30.0);
        overall.setSummary("Excellent");

        evaluation.setCorrectness(correctness);
        evaluation.setEfficiency(efficiency);
        evaluation.setStyle(style);
        evaluation.setOverall(overall);

        response.setEvaluation(evaluation);
        return response;
    }

    private EssaySubmissionFeedback createEssaySubmissionFeedback() {
        EssaySubmissionFeedback feedback = new EssaySubmissionFeedback();
        EssaySubmissionFeedback.Evaluation evaluation = new EssaySubmissionFeedback.Evaluation();

        EssaySubmissionFeedback.CategoryEvaluation completeness =
                EssaySubmissionFeedback.CategoryEvaluation.builder()
                        .score(25.0)
                        .percentage(100)
                        .feedback("Complete response")
                        .build();

        EssaySubmissionFeedback.OverallEvaluation overall =
                EssaySubmissionFeedback.OverallEvaluation.builder()
                        .totalScore(100.0)
                        .percentage(100.0)
                        .passed(true)
                        .summary("Excellent essay")
                        .build();

        evaluation.setCompleteness(completeness);
        evaluation.setOverall(overall);
        feedback.setEvaluation(evaluation);
        return feedback;
    }

    private Judge0SubmissionResponse createJudge0Response(
            int statusId, String statusDescription, String stdout, String stderr, String compileOutput
    ) {
        Judge0SubmissionResponse response = new Judge0SubmissionResponse();
        Judge0SubmissionResponse.Judge0Status status = new Judge0SubmissionResponse.Judge0Status();
        status.setId(statusId);
        status.setDescription(statusDescription);

        response.setStatus(status);
        response.setStdout(stdout);
        response.setStderr(stderr);
        response.setCompileOutput(compileOutput);
        return response;
    }

    private String createDetailedEvaluationJson() {
        return "{ \"evaluation\": { " +
                "\"correctness\": { \"score\": 10, \"feedback\": \"Good\", \"testResults\": [{ \"passed\": true }] }, " +
                "\"efficiency\": { \"score\": 10, \"feedback\": \"Good\" }, " +
                "\"style\": { \"score\": 10, \"feedback\": \"Good\" }, " +
                "\"overall\": { \"totalScore\": 30, \"summary\": \"Excellent\" } " +
                "} }";
    }

    private String createSimpleFeedbackJson() {
        return "{ " +
                "\"passedTests\": 3, " +
                "\"totalTests\": 3, " +
                "\"correctnessFeedback\": \"Good\", " +
                "\"efficiencyFeedback\": \"Good\", " +
                "\"styleFeedback\": \"Good\", " +
                "\"overallSuggestion\": \"Excellent\" " +
                "}";
    }

    private String createEssayEvaluationJson() {
        return "{ \"evaluation\": { " +
                "\"completeness\": { \"score\": 25, \"percentage\": 100, \"feedback\": \"Complete\" }, " +
                "\"accuracy\": { \"score\": 25, \"percentage\": 100, \"feedback\": \"Accurate\" }, " +
                "\"clarity\": { \"score\": 25, \"percentage\": 100, \"feedback\": \"Clear\" }, " +
                "\"depth\": { \"score\": 25, \"percentage\": 100, \"feedback\": \"Deep\" }, " +
                "\"overall\": { \"totalScore\": 100, \"percentage\": 100, \"passed\": true, \"summary\": \"Excellent\" } " +
                "} }";
    }
}
