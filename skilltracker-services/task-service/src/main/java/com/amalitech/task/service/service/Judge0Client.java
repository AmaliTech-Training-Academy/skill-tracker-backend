package com.amalitech.task.service.service;

import com.amalitech.task.service.dto.client.request.Judge0SubmissionRequest;
import com.amalitech.task.service.dto.client.response.Judge0SubmissionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Client for communicating with the self-hosted Judge0 API.
 * Responsible for executing user code against a single test case.
 */
@Service
@Slf4j
public class Judge0Client {

    private final WebClient judge0ApiClient;

    public Judge0Client(@Qualifier("judge0ApiWebClient") WebClient judge0ApiClient) {
        this.judge0ApiClient = judge0ApiClient;
    }

    /**
     * Executes a single submission (one test case) and waits for the result.
     *
     * @param request The request containing code, languageId, stdin, and expected_output.
     * @return A Mono containing the raw execution result from Judge0.
     */
    public Mono<Judge0SubmissionResponse> executeSubmission(Judge0SubmissionRequest request) {
        log.info("Sending submission to Judge0: languageId={}, codeLength={}",
                request.getLanguageId(),
                request.getSourceCode() != null ? request.getSourceCode().length() : 0);

        return judge0ApiClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/submissions")
                        .queryParam("base64_encoded", "false")
                        .queryParam("wait", "true")
                        .build())
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Judge0SubmissionResponse.class)
                .doOnSuccess(response -> log.info("Received Judge0 response, status: {}", response.getStatus().getDescription()))
                .doOnError(e -> log.error("Failed to execute Judge0 submission: {}", e.getMessage()));
    }
}
