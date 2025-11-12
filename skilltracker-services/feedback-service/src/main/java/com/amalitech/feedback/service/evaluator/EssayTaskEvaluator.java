package com.amalitech.feedback.service.evaluator;

import com.amalitech.common.event.events.SubmissionCreatedEvent;
import com.amalitech.common.event.events.SubmissionEvaluatedEvent;
import com.amalitech.feedback.service.dto.client.TaskDTO;
import com.amalitech.feedback.service.dto.client.submission.impl.EssaySubmissionFeedback;
import com.amalitech.feedback.service.service.AIFeedbackClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Evaluator for ESSAY tasks.
 * Analyzes written responses using AI evaluation.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EssayTaskEvaluator implements TaskEvaluator {

    private final AIFeedbackClient aiFeedbackClient;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<SubmissionEvaluatedEvent> evaluate(SubmissionCreatedEvent event) {
        log.info("Evaluating ESSAY task submission: {}", event.getSubmissionId());

        return generateEssayFeedback(event)
                .map(feedback -> buildSuccessEvent(event, feedback))
                .onErrorResume(e -> {
                    log.error("AI essay evaluation failed for {}: {}", event.getSubmissionId(), e.getMessage());
                    return Mono.just(buildFallbackEvent(event));
                });
    }

    @Override
    public String getTaskType() {
        return "ESSAY";
    }

    /**
     * Generates AI feedback for the essay submission.
     */
    private Mono<EssaySubmissionFeedback> generateEssayFeedback(SubmissionCreatedEvent event) {
        TaskDTO task = buildTaskDTO(event);

        return aiFeedbackClient.generateEssayFeedback(task, event);
    }

    /**
     * Builds the evaluated event with AI essay feedback.
     * <p>
     * This method validates that the feedback structure contains all required
     * category evaluations and overall results. If any required field is null,
     * it logs a warning and falls back to default values.
     */
    private SubmissionEvaluatedEvent buildSuccessEvent(
            SubmissionCreatedEvent event,
            EssaySubmissionFeedback feedback
    ) {
        if (feedback == null || feedback.getEvaluation() == null) {
            log.warn("Feedback or evaluation is null for submission: {}", event.getSubmissionId());
            return buildFallbackEvent(event);
        }

        EssaySubmissionFeedback.Evaluation eval = feedback.getEvaluation();
        EssaySubmissionFeedback.OverallEvaluation overall = eval.getOverall();

        if (overall == null) {
            log.warn("Overall evaluation is null for submission: {}", event.getSubmissionId());
            return buildFallbackEvent(event);
        }

        String overallFeedback = buildFeedbackString(eval, overall);

        int score = overall.getPercentage() != null ? overall.getPercentage().intValue() : 0;
        boolean passed = overall.getPassed() != null ? overall.getPassed() : false;

        return SubmissionEvaluatedEvent.builder()
                .submissionId(event.getSubmissionId())
                .userId(event.getUserId())
                .status("COMPLETED")
                .score(score)
                .isCorrect(passed)
                .feedbackType("ESSAY")
                .overallFeedback(overallFeedback)
                .detailedFeedback(serializeDetailedFeedback(feedback))
                .avgExecutionTimeMs(0.0)
                .avgMemoryUsedKb(0)
                .build();
    }

    /**
     * Builds the overall feedback string from evaluation categories.
     * Safely extracts feedback from each category with null-checking.
     *
     * @param eval The evaluation containing category details.
     * @param overall The overall evaluation summary.
     * @return A formatted feedback string combining all categories.
     */
    private String buildFeedbackString(
            EssaySubmissionFeedback.Evaluation eval,
            EssaySubmissionFeedback.OverallEvaluation overall
    ) {
        String completeness = eval.getCompleteness() != null ? eval.getCompleteness().getFeedback() : "N/A";
        String accuracy = eval.getAccuracy() != null ? eval.getAccuracy().getFeedback() : "N/A";
        String clarity = eval.getClarity() != null ? eval.getClarity().getFeedback() : "N/A";
        String depth = eval.getDepth() != null ? eval.getDepth().getFeedback() : "N/A";
        String summary = overall.getSummary() != null ? overall.getSummary() : "Evaluation completed.";
        String nextSteps = overall.getNextSteps() != null ? overall.getNextSteps() : "No additional steps provided.";

        return String.format(
                "Completeness: %s\\nAccuracy: %s\\nClarity: %s\\nDepth: %s\\n\\nOverall: %s\\n\\nNext Steps: %s",
                completeness, accuracy, clarity, depth, summary, nextSteps
        );
    }

    /**
     * Builds a fallback event when AI evaluation fails.
     */
    private SubmissionEvaluatedEvent buildFallbackEvent(SubmissionCreatedEvent event) {
        return SubmissionEvaluatedEvent.builder()
                .submissionId(event.getSubmissionId())
                .userId(event.getUserId())
                .status("COMPLETED")
                .score(0)
                .isCorrect(false)
                .feedbackType("ESSAY")
                .overallFeedback("Essay evaluation completed, but AI feedback is unavailable. Please review the submission manually.")
                .avgExecutionTimeMs(0.0)
                .avgMemoryUsedKb(0)
                .build();
    }

    /**
     * Builds a minimal TaskDTO from the event for AI feedback.
     */
    private TaskDTO buildTaskDTO(SubmissionCreatedEvent event) {
        return TaskDTO.builder()
                .id(event.getTaskId())
                .description("Essay task for submission " + event.getSubmissionId())
                .build();
    }

    /**
     * Serializes the detailed essay feedback to a JSON string.
     * This now constructs a Map to explicitly include the
     * 'feedbackType' property, which is required by the
     * task-service's polymorphic deserializer.
     */
    private String serializeDetailedFeedback(EssaySubmissionFeedback feedback) {
        try {
            Map<String, Object> polymorphicFeedback = new HashMap<>();

            polymorphicFeedback.put("feedbackType", "ESSAY");
            polymorphicFeedback.put("evaluation", feedback.getEvaluation());

            return objectMapper.writeValueAsString(polymorphicFeedback);

        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize detailed essay feedback map: {}", e.getMessage());
            return null;
        }
    }
}
