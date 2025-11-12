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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client for communicating with the OpenAI API using Spring AI.
 * Responsible for generating human-friendly feedback from raw execution results.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AIFeedbackClient {

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper;
    private final PromptTemplate codingEvaluationPromptTemplate;
    private final PromptTemplate simpleFeedbackPromptTemplate;
    private final PromptTemplate writtenEvaluationPromptTemplate;

    /**
     * Generates AI feedback for a user's coding submission using the detailed prompt template.
     *
     * @return A Mono containing the structured DetailedEvaluationResponse object.
     */
    public Mono<DetailedEvaluationResponse> generateDetailedFeedback(
            TaskDTO task,
            String userCode,
            List<Judge0SubmissionResponse> executionResults
    ) {
        log.info("Generating detailed AI feedback for task: {}", task.getId());

        return Mono.fromCallable(() -> {
            ChatClient chatClient = chatClientBuilder.build();

            Map<String, Object> variables = new HashMap<>();
            variables.put("skill", task.getSkillName() != null ? task.getSkillName() : "General Programming");
            variables.put("difficulty", task.getDifficulty() != null ? task.getDifficulty().toString() : "MEDIUM");
            variables.put("userCode", userCode);
            variables.put("requirements", task.getDescription() != null ? task.getDescription() : "No requirements provided");
            variables.put("testCases", formatTestCases(executionResults));
            variables.put("criteria", "Standard evaluation criteria: correctness, efficiency, and code style");

            Prompt prompt = codingEvaluationPromptTemplate.create(variables);

            String response = chatClient.prompt(prompt)
                    .call()
                    .content();

            return parseDetailedResponse(response);
        })
        .doOnError(e -> log.error("Failed to generate AI feedback: {}", e.getMessage()));
    }

    /**
     * Generates AI feedback for a user's coding submission (legacy method for backward compatibility).
     *
     * @return A Mono containing the structured CodingSubmissionFeedback object.
     */
    public Mono<CodingSubmissionFeedback> generateCodingFeedback(
            TaskDTO task,
            String userCode,
            List<Judge0SubmissionResponse> executionResults
    ) {
        return generateDetailedFeedback(task, userCode, executionResults)
                .map(this::convertToSimpleFeedback)
                .onErrorResume(e -> {
                    log.error("Detailed feedback failed, falling back to simple feedback", e);
                    return generateSimpleFeedback(task, userCode, executionResults);
                });
    }

    /**
     * Generates AI feedback for a user's coding submission using execution results.
     *
     * @return A Mono containing the structured CodingSubmissionFeedback object.
     */
    public Mono<CodingSubmissionFeedback> generateCodingFeedback(
            TaskDTO task,
            SubmissionCreatedEvent event,
            List<Judge0SubmissionResponse> executionResults
    ) {
        String userCode = event.getContentToEvaluate();
        return generateCodingFeedback(task, userCode, executionResults);
    }

    /**
     * Generates AI feedback for a user's essay submission.
     *
     * @param task the task information
     * @param event the submission event containing all details including content
     * @return A Mono containing the structured EssaySubmissionFeedback object.
     */
    public Mono<EssaySubmissionFeedback> generateEssayFeedback(
            TaskDTO task,
            SubmissionCreatedEvent event
    ) {
        log.info("Generating AI essay feedback for task: {}", task.getId());

        return Mono.fromCallable(() -> {
            ChatClient chatClient = chatClientBuilder.build();

            Map<String, Object> variables = new HashMap<>();
            variables.put("skill", event.getSkillName() != null ? event.getSkillName() : "General Writing");
            variables.put("difficulty", event.getDifficulty() != null ? event.getDifficulty() : "INTERMEDIATE");
            variables.put("title", event.getTaskTitle() != null ? event.getTaskTitle() : "Essay Task");
            variables.put("prompt", event.getTaskDescription() != null ? event.getTaskDescription() : "Write an essay response");
            variables.put("userResponse", event.getContentToEvaluate());
            variables.put("detailedInstructions", event.getDetailedInstructions() != null ?
                    event.getDetailedInstructions() : "Please provide a well-structured essay response addressing the topic.");
            variables.put("evaluationCriteria", event.getEvaluationCriteria() != null ?
                    event.getEvaluationCriteria() : "Standard essay evaluation criteria focusing on completeness, accuracy, clarity, and depth");
            variables.put("rubric", event.getRubric() != null ?
                    event.getRubric() : "Comprehensive rubric covering all evaluation dimensions");

            Prompt prompt = writtenEvaluationPromptTemplate.create(variables);

            String response = chatClient.prompt(prompt)
                    .call()
                    .content();

            return parseEssayEvaluationResponse(response);
        })
        .doOnError(e -> log.error("Failed to generate essay AI feedback: {}", e.getMessage()));
    }

    /**
     * Fallback method for generating simple feedback when detailed feedback fails.
     */
    private Mono<CodingSubmissionFeedback> generateSimpleFeedback(
            TaskDTO task,
            String userCode,
            List<Judge0SubmissionResponse> executionResults
    ) {
        return Mono.fromCallable(() -> {
            ChatClient chatClient = chatClientBuilder.build();

            Map<String, Object> variables = new HashMap<>();
            variables.put("description", task.getDescription() != null ? task.getDescription() : "No description provided");
            variables.put("userCode", userCode);
            variables.put("executionResults", formatExecutionResults(executionResults));

            Prompt prompt = simpleFeedbackPromptTemplate.create(variables);

            String response = chatClient.prompt(prompt)
                    .call()
                    .content();

            return parseSimpleAiResponse(response);
        });
    }

    /**
     * Parses the detailed JSON response from AI.
     */
    private DetailedEvaluationResponse parseDetailedResponse(String jsonContent) {
    try {
    if (jsonContent == null || jsonContent.isBlank()) {
    throw new InvalidAiResponseException("AI response was empty.");
    }

    // Clean up markdown and whitespace
    jsonContent = jsonContent.replace("```json", "")
    .replace("```", "")
    .trim();

    DetailedEvaluationResponse response = objectMapper.readValue(
    jsonContent, DetailedEvaluationResponse.class
    );

    validateEvaluationResponse(response);

    return response;

    } catch (InvalidAiResponseException e) {
    throw e;
    } catch (Exception e) {
        log.error("Failed to parse detailed AI JSON response: {}", e.getMessage());
            throw new AiResponseParsingException("Failed to parse AI response.", e);
         }
     }

    private void validateEvaluationResponse(DetailedEvaluationResponse response) {
    if (response.getEvaluation() == null) {
    throw new InvalidAiResponseException("Missing evaluation object");
    }

    var eval = response.getEvaluation();

    if (eval.getCorrectness() == null || eval.getEfficiency() == null ||
    eval.getStyle() == null || eval.getOverall() == null) {
    throw new InvalidAiResponseException("Missing evaluation categories");
    }

        double calculatedTotal = eval.getCorrectness().getScore() +
                eval.getEfficiency().getScore() +
                eval.getStyle().getScore();

        if (Math.abs(calculatedTotal - eval.getOverall().getTotalScore()) > 0.1) {
            log.warn("Score mismatch: calculated {} but overall is {}",
                    calculatedTotal, eval.getOverall().getTotalScore());
        }
    }

    /**
     * Parses the simple JSON response from AI (fallback).
     */
    private CodingSubmissionFeedback parseSimpleAiResponse(String jsonContent) {
    try {
    if (jsonContent == null || jsonContent.isBlank()) {
    throw new InvalidAiResponseException("AI response was empty.");
    }

    jsonContent = jsonContent.replace("```json", "").replace("```", "").trim();

        return objectMapper.readValue(jsonContent, CodingSubmissionFeedback.class);

    } catch (InvalidAiResponseException e) {
    throw e;
    } catch (Exception e) {
        log.error("Failed to parse AI JSON response: {}", e.getMessage());
            throw new AiResponseParsingException("Failed to parse AI response.", e);
         }
     }

    /**
     * Converts detailed evaluation response to simple feedback format.
     */
    private CodingSubmissionFeedback convertToSimpleFeedback(DetailedEvaluationResponse detailed) {
        DetailedEvaluationResponse.Evaluation eval = detailed.getEvaluation();
        
        return CodingSubmissionFeedback.builder()
                .correctnessFeedback(eval.getCorrectness().getFeedback())
                .efficiencyFeedback(eval.getEfficiency().getFeedback())
                .styleFeedback(eval.getStyle().getFeedback())
                .overallSuggestion(eval.getOverall().getSummary())
                .passedTests(countPassedTests(eval.getCorrectness().getTestResults()))
                .totalTests(eval.getCorrectness().getTestResults() != null ? eval.getCorrectness().getTestResults().size() : 0)
                .build();
    }

    /**
     * Counts passed tests from detailed evaluation.
     */
    private int countPassedTests(List<DetailedEvaluationResponse.TestResult> testResults) {
        if (testResults == null) return 0;
        return (int) testResults.stream().filter(DetailedEvaluationResponse.TestResult::isPassed).count();
    }

    /**
     * Formats test cases for the detailed prompt.
     */
    private String formatTestCases(List<Judge0SubmissionResponse> executionResults) {
        if (executionResults == null || executionResults.isEmpty()) {
            return "No test cases available";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < executionResults.size(); i++) {
            Judge0SubmissionResponse result = executionResults.get(i);
            sb.append(String.format("TEST %d:\n", i + 1));
            sb.append(String.format("  Status: %s (ID: %d)\n",
                    result.getStatus() != null ? result.getStatus().getDescription() : "Unknown",
                    result.getStatus() != null ? result.getStatus().getId() : 0));
            sb.append(String.format("  Output: %s\n",
                    result.getStdout() != null ? result.getStdout() : "None"));
            sb.append(String.format("  Error: %s\n",
                    result.getStderr() != null ? result.getStderr() : "None"));
            if (result.getCompileOutput() != null && !result.getCompileOutput().isBlank()) {
                sb.append(String.format("  Compile Output: %s\n", result.getCompileOutput()));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Formats execution results for the simple prompt.
     */
    private String formatExecutionResults(List<Judge0SubmissionResponse> executionResults) {
        if (executionResults == null || executionResults.isEmpty()) {
            return "No execution results available";
        }

        Judge0SubmissionResponse firstFailed = executionResults.stream()
                .filter(r -> r.getStatus() != null && r.getStatus().getId() != 3) // 3 = "Accepted"
                .findFirst()
                .orElse(null);

        StringBuilder sb = new StringBuilder();
        
        if (firstFailed != null) {
            sb.append("FAILED TEST CASE:\n");
            sb.append("Status: ").append(firstFailed.getStatus().getDescription()).append("\n");

            if (firstFailed.getStderr() != null && !firstFailed.getStderr().isBlank()) {
                sb.append("Error: ").append(firstFailed.getStderr()).append("\n");
            }

            if (firstFailed.getCompileOutput() != null && !firstFailed.getCompileOutput().isBlank()) {
                sb.append("Compile Output: ").append(firstFailed.getCompileOutput()).append("\n");
            }

            if (firstFailed.getStdout() != null) {
                sb.append("Actual Output: ").append(firstFailed.getStdout()).append("\n");
            }
        } else {
            sb.append("All test cases passed successfully!");
        }

        return sb.toString();
    }

    /**
     * Parses the essay evaluation JSON response from AI.
     */
    private EssaySubmissionFeedback parseEssayEvaluationResponse(String jsonContent) {
    try {
    if (jsonContent == null || jsonContent.isBlank()) {
    throw new InvalidAiResponseException("AI response was empty.");
    }

    jsonContent = jsonContent.replace("```json", "")
    .replace("```", "")
    .trim();

    return objectMapper.readValue(
    jsonContent, EssaySubmissionFeedback.class
    );

    } catch (InvalidAiResponseException e) {
    throw e;
    } catch (Exception e) {
        log.error("Failed to parse essay AI JSON response: {}", e.getMessage());
            throw new AiResponseParsingException("Failed to parse AI essay response.", e);
         }
     }
}