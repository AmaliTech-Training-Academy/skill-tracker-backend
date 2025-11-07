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
     */
    private SubmissionEvaluatedEvent buildSuccessEvent(
            SubmissionCreatedEvent event,
            EssaySubmissionFeedback feedback
    ) {
        EssaySubmissionFeedback.Evaluation eval = feedback.getEvaluation();
        EssaySubmissionFeedback.OverallEvaluation overall = eval.getOverall();

        String overallFeedback = String.format(
                "Completeness: %s\\nAccuracy: %s\\nClarity: %s\\nDepth: %s\\n\\nOverall: %s\\n\\nNext Steps: %s",
                eval.getCompleteness().getFeedback(),
                eval.getAccuracy().getFeedback(),
                eval.getClarity().getFeedback(),
                eval.getDepth().getFeedback(),
                overall.getSummary(),
                overall.getNextSteps()
        );

        return SubmissionEvaluatedEvent.builder()
                .submissionId(event.getSubmissionId())
                .userId(event.getUserId())
                .status("COMPLETED")
                .score(overall.getPercentage().intValue())
                .isCorrect(overall.getPassed())
                .feedbackType("ESSAY")
                .overallFeedback(overallFeedback)
                .detailedFeedback(serializeDetailedFeedback(feedback))
                .avgExecutionTimeMs(0.0) // Essays don't have execution time
                .avgMemoryUsedKb(0) // Essays don't use memory
                .build();
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
