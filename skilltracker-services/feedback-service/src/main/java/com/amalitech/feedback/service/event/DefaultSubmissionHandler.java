package com.amalitech.feedback.service.event;

import com.amalitech.common.event.events.SubmissionCreatedEvent;
import com.amalitech.common.event.events.SubmissionEvaluatedEvent;
import com.amalitech.feedback.service.config.RabbitMQConfig;
import com.amalitech.feedback.service.dto.client.GradingData;
import com.amalitech.feedback.service.dto.client.request.Judge0SubmissionRequest;
import com.amalitech.feedback.service.dto.client.response.Judge0SubmissionResponse;
import com.amalitech.feedback.service.service.AIFeedbackClient;
import com.amalitech.feedback.service.service.Judge0Client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This is the core "assembly line" of the feedback-service.
 * It implements the SubmissionHandler interface and contains all
 * orchestration logic, independent of any message broker.
 */
@Service
@Primary
@Slf4j
@RequiredArgsConstructor
public class DefaultSubmissionHandler implements SubmissionHandler {

    private final RabbitTemplate rabbitTemplate;
    private final Judge0Client judge0Client;
    private final AIFeedbackClient aiFeedbackClient;

    /**
     * Processes a single submission for evaluation.
     */
    @Override
    public void handleSubmission(SubmissionCreatedEvent event) {
        log.info("Handling submission for evaluation: {}", event.getSubmissionId());

        if (!"CODING".equals(event.getTaskType())) {
            log.warn("Received non-coding submission, skipping. ID: {}", event.getSubmissionId());
            // TODO: Publish a "FAILED" event for non-coding tasks
            return;
        }

        // This reactive chain is perfect.
        Mono.just(event)
                .flatMap(this::runTestCases)
                .flatMap(this::gradeAndProvideFeedback)
                .flatMap(this::publishEvaluatedEvent)
                .onErrorResume(e -> handleError(event, e))
                .subscribe(
                        v -> log.info("Successfully completed and published evaluation for: {}", event.getSubmissionId()),
                        e -> log.error("Unhandled error in subscription for {}: {}", event.getSubmissionId(), e.getMessage())
                );
    }

    /**
     * 2. Runs all test cases against Judge0 sequentially.
     * This is perfectly refactored to use only event data.
     */
    private Mono<GradingData> runTestCases(SubmissionCreatedEvent event) {
        List<SubmissionCreatedEvent.TestCaseData> testCases = event.getTestCases();

        if (testCases == null || testCases.isEmpty()) {
            return Mono.error(new RuntimeException("Task " + event.getTaskId() + " has no test cases in the event."));
        }

        return Flux.fromIterable(testCases)
                .concatMap(testCase -> {
                    Judge0SubmissionRequest request = Judge0SubmissionRequest.builder()
                            .languageId(event.getLanguageId())
                            .sourceCode(event.getCodeToEvaluate())
                            .stdin(testCase.getInput())
                            .expectedOutput(testCase.getExpectedOutput())
                            .build();
                    return judge0Client.executeSubmission(request);
                })
                .collectList()
                .map(results -> new GradingData(event, results));
    }

    /**
     * 3. Grades the raw results and provides feedback.
     */
    private Mono<SubmissionEvaluatedEvent> gradeAndProvideFeedback(GradingData data) {
        List<Judge0SubmissionResponse> results = data.judge0Results();
        SubmissionCreatedEvent event = data.event();

        int totalTests = results.size();
        int passedTests = (int) results.stream().filter(r -> r.getStatus() != null && r.getStatus().getId() == 3).count();
        boolean isCorrect = totalTests > 0 && passedTests == totalTests;

        Judge0SubmissionResponse firstError = results.stream()
                .filter(r -> r.getStatus() != null && r.getStatus().getId() != 3)
                .findFirst()
                .orElse(results.isEmpty() ? null : results.get(0));

        log.warn("STUBBING AI FEEDBACK for submission: {}", data.event().getSubmissionId());

        String statusDescription = "Processing Error";
        double execTime = 0;
        int execMemory = 0;

        if (firstError != null) {
            if (firstError.getStatus() != null) {
                statusDescription = firstError.getStatus().getDescription();
            }
            if (firstError.getTime() != null) {
                execTime = firstError.getTime() * 1000;
            }
            if (firstError.getMemory() != null) {
                execMemory = firstError.getMemory();
            }
        } else if (isCorrect) {
            statusDescription = "All Tests Passed";
            execTime = results.stream().mapToDouble(r -> r.getTime() != null ? r.getTime() * 1000 : 0).average().orElse(0);
            execMemory = (int) results.stream().mapToDouble(r -> r.getMemory() != null ? r.getMemory() : 0).average().orElse(0);
        }

        // TODO: Here you would call:
        // Mono<String> aiFeedback = aiFeedbackClient.getFeedback(event.getCodeToEvaluate(), results);
        // ... and then map it into the event builder ...

        SubmissionEvaluatedEvent evaluatedEvent = SubmissionEvaluatedEvent.builder()
                .submissionId(event.getSubmissionId())
                .userId(event.getUserId())
                .status(isCorrect ? "COMPLETED" : "COMPLETED")
                .score(isCorrect ? 100 : (int) (((double) passedTests / totalTests) * 100))
                .isCorrect(isCorrect)
                .feedbackType("CODING")
                .overallFeedback("AI feedback is pending. Status: " + statusDescription)
                .testCaseResults(results.stream()
                        .map(r -> "Test " + (r.getStatus() != null ? r.getStatus().getDescription() : "Error"))
                        .collect(Collectors.toList()))
                .build();

        return Mono.just(evaluatedEvent);
    }

    /**
     * Publishes the final evaluated result to RabbitMQ.
     * This is perfect.
     */
    private Mono<Void> publishEvaluatedEvent(SubmissionEvaluatedEvent evaluatedEvent) {
        log.info("Publishing submission.evaluated event for ID: {}", evaluatedEvent.getSubmissionId());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SUBMISSION_EXCHANGE,
                RabbitMQConfig.SUBMISSION_EVALUATED_ROUTING_KEY,
                evaluatedEvent
        );

        return Mono.empty();
    }

    /**
     * Publishes an error event.
     * This is perfect, robust error handling.
     */
    private Mono<Void> handleError(SubmissionCreatedEvent event, Throwable error) {
        log.error("CRITICAL ERROR processing submission {}: {}", event.getSubmissionId(), error.getMessage(), error);

        SubmissionEvaluatedEvent errorEvent = SubmissionEvaluatedEvent.builder()
                .submissionId(event.getSubmissionId())
                .userId(event.getUserId())
                .status("ERROR")
                .score(0)
                .isCorrect(false)
                .feedbackType(event.getTaskType())
                .overallFeedback("An unexpected error occurred during evaluation: " + error.getMessage())
                .build();

        // Re-use your publish method
        return publishEvaluatedEvent(errorEvent);
    }


}