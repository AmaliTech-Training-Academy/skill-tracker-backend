package com.amalitech.feedback.service.service;

import com.amalitech.feedback.service.dto.client.TaskDTO;
import com.amalitech.feedback.service.dto.client.response.Judge0SubmissionResponse;
import com.amalitech.feedback.service.dto.client.submission.impl.CodingSubmissionFeedback;
import com.amalitech.feedback.service.dto.client.submission.DetailedEvaluationResponse;

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
import java.util.stream.Collectors;

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
    public Mono<CodingSubmissionFeedback> generateFeedback(
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
                throw new RuntimeException("AI response was empty.");
            }

            jsonContent = jsonContent.replace("```json", "").replace("```", "").trim();

            return objectMapper.readValue(jsonContent, DetailedEvaluationResponse.class);

        } catch (Exception e) {
            log.error("Failed to parse detailed AI JSON response: {}", e.getMessage());
            throw new RuntimeException("Failed to parse AI response.", e);
        }
    }

    /**
     * Parses the simple JSON response from AI (fallback).
     */
    private CodingSubmissionFeedback parseSimpleAiResponse(String jsonContent) {
        try {
            if (jsonContent == null || jsonContent.isBlank()) {
                throw new RuntimeException("AI response was empty.");
            }

            jsonContent = jsonContent.replace("```json", "").replace("```", "").trim();

            CodingSubmissionFeedback feedback = objectMapper.readValue(jsonContent, CodingSubmissionFeedback.class);
            feedback.setAiModelUsed("gpt-4o-mini");

            return feedback;

        } catch (Exception e) {
            log.error("Failed to parse AI JSON response: {}", e.getMessage());
            throw new RuntimeException("Failed to parse AI response.", e);
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
                .aiModelUsed("gpt-4o-mini")
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

        return executionResults.stream()
                .map(result -> String.format(
                        "Status: %s, Output: %s, Error: %s",
                        result.getStatus() != null ? result.getStatus().getDescription() : "Unknown",
                        result.getStdout() != null ? result.getStdout() : "None",
                        result.getStderr() != null ? result.getStderr() : "None"
                ))
                .collect(Collectors.joining("\n"));
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
}