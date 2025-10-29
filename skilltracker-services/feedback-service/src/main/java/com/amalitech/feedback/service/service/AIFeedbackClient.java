package com.amalitech.feedback.service.service;

import com.amalitech.feedback.service.dto.client.TaskDTO;
import com.amalitech.feedback.service.dto.client.request.DeepSeekRequest;
import com.amalitech.feedback.service.dto.client.response.DeepSeekResponse;
import com.amalitech.feedback.service.dto.client.response.Judge0SubmissionResponse;
import com.amalitech.feedback.service.dto.client.submission.CodingSubmissionFeedback;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Client for communicating with the DeepSeek AI API.
 * Responsible for generating human-friendly feedback from raw execution results.
 */
@Service
@Slf4j
public class AIFeedbackClient {

    private final WebClient deepseekApiClient;
    private final ObjectMapper objectMapper;

    @Value("${client.deepseek-api.model:deepseek-chat}")
    private String aiModel;

    public AIFeedbackClient(
            @Qualifier("deepseekApiWebClient") WebClient deepseekApiClient,
            ObjectMapper objectMapper
    ) {
        this.deepseekApiClient = deepseekApiClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Generates AI feedback for a user's coding submission.
     *
     * @return A Mono containing the structured CodingSubmissionFeedback object.
     */
    public Mono<CodingSubmissionFeedback> generateFeedback(
            TaskDTO task,
            String userCode,
            List<Judge0SubmissionResponse> executionResults
    ) {
        log.info("Generating AI feedback for task: {}", task.getId());
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(task, userCode, executionResults);

        DeepSeekRequest aiRequest = DeepSeekRequest.builder()
                .model(aiModel)
                .messages(List.of(
                        DeepSeekRequest.Message.builder().role("system").content(systemPrompt).build(),
                        DeepSeekRequest.Message.builder().role("user").content(userPrompt).build()
                ))
                .build();

        return deepseekApiClient.post()
                .uri("/chat/completions")
                .bodyValue(aiRequest)
                .retrieve()
                .bodyToMono(DeepSeekResponse.class)
                .flatMap(response -> parseAiResponse(response, executionResults))
                .doOnError(e -> log.error("Failed to generate AI feedback: {}", e.getMessage()));
    }

    /**
     * Parses the raw JSON string from the AI into our DTO.
     */
    private Mono<CodingSubmissionFeedback> parseAiResponse(
            DeepSeekResponse response,
            List<Judge0SubmissionResponse> executionResults
    ) {
        try {
            String jsonContent = response.getFirstChoiceContent();
            if (jsonContent == null) {
                return Mono.error(new RuntimeException("AI response was empty."));
            }

            jsonContent = jsonContent.replace("```json", "").replace("```", "").trim();

            CodingSubmissionFeedback feedback = objectMapper.readValue(jsonContent, CodingSubmissionFeedback.class);

            feedback.setAiModelUsed(aiModel);

            return Mono.just(feedback);

        } catch (Exception e) {
            log.error("Failed to parse AI JSON response: {}", e.getMessage());
            return Mono.error(new RuntimeException("Failed to parse AI response.", e));
        }
    }

    // --- Prompt Engineering ---

    private String buildSystemPrompt() {
        return "You are an expert, encouraging programming coach. " +
                "Your goal is to provide feedback on a user's code submission. " +
                "You will receive the problem description, the user's code, and a " +
                "list of execution results from the test cases. " +
                "You MUST respond ONLY with a single, valid JSON object " +
                "matching this exact structure: \n" +
                "{\n" +
                "  \"correctnessFeedback\": \"(string) Feedback on why the code failed or passed. Be specific about the failed test case.\",\n" +
                "  \"efficiencyFeedback\": \"(string) Feedback on the O-notation and memory usage. Is it optimal?\",\n" +
                "  \"styleFeedback\": \"(string) Feedback on code readability, naming, and best practices.\",\n" +
                "  \"overallSuggestion\": \"(string) A single, actionable tip for the user to improve.\"\n" +
                "}\n" +
                "Do NOT use markdown. Do NOT add any text before or after the JSON.";
    }

    private String buildUserPrompt(
            TaskDTO task,
            String userCode,
            List<Judge0SubmissionResponse> executionResults
    ) {
        // We only show the first failed test case to the AI to save tokens and get focused feedback.
        Judge0SubmissionResponse firstFailed = executionResults.stream()
                .filter(r -> r.getStatus().getId() != 3) // 3 = "Accepted"
                .findFirst()
                .orElse(null);

        StringBuilder sb = new StringBuilder();
        sb.append("--- PROBLEM DESCRIPTION ---\n").append(task.getDescription()).append("\n");
        sb.append("--- USER'S CODE ---\n").append(userCode).append("\n");

        if (firstFailed != null) {
            sb.append("--- FAILED TEST CASE --- \n");
            sb.append("Status: ").append(firstFailed.getStatus().getDescription()).append("\n");
            if (firstFailed.getStderr() != null) {
                sb.append("Stderr: ").append(firstFailed.getStderr()).append("\n");
            }
            if (firstFailed.getCompileOutput() != null) {
                sb.append("Compile Output: ").append(firstFailed.getCompileOutput()).append("\n");
            }
        } else {
            sb.append("--- EXECUTION RESULT ---\nAll test cases passed successfully!\n");
        }

        return sb.toString();
    }
}