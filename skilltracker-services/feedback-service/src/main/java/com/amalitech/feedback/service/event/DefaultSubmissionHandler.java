package com.amalitech.feedback.service.event;

import com.amalitech.common.event.events.SubmissionCreatedEvent;
import com.amalitech.common.event.events.SubmissionEvaluatedEvent;
import com.amalitech.feedback.service.config.RabbitMQConfig;
import com.amalitech.feedback.service.evaluator.TaskEvaluatorFactory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * This is the core "assembly line" of the feedback-service.
 * It implements the SubmissionHandler interface and delegates
 * task-specific evaluation to the appropriate evaluator via the factory.
 */
@Service
@Primary
@Slf4j
@RequiredArgsConstructor
public class DefaultSubmissionHandler implements SubmissionHandler {

    private final RabbitTemplate rabbitTemplate;
    private final TaskEvaluatorFactory evaluatorFactory;

    /**
     * Processes a single submission for evaluation.
     * 
     * <p>Routes the submission to the appropriate task evaluator based on task type,
     * publishes the evaluation result, and handles any errors that occur during processing.</p>
     * 
     * @param event the submission event to evaluate
     */
    @Override
    public void handleSubmission(SubmissionCreatedEvent event) {
        log.info("Handling submission for evaluation: {}", event.getSubmissionId());

        try {
            evaluatorFactory.getEvaluator(event.getTaskType())
                    .evaluate(event)
                    .flatMap(this::publishEvaluatedEvent)
                    .onErrorResume(e -> handleError(event, e))
                    .subscribe(
                            v -> log.info("Successfully completed and published evaluation for: {}", event.getSubmissionId()),
                            e -> log.error("Unhandled error in subscription for {}: {}", event.getSubmissionId(), e.getMessage())
                    );
        } catch (IllegalArgumentException e) {
            log.error("Unsupported task type for submission {}: {}", event.getSubmissionId(), e.getMessage());
            handleError(event, e).subscribe();
        }
    }

    /**
     * Publishes the final evaluated result to RabbitMQ.
     * 
     * <p>Sends the SubmissionEvaluatedEvent to the submission exchange with the evaluated
     * routing key, making the results available to downstream consumers.</p>
     * 
     * @param evaluatedEvent the completed evaluation event to publish
     * @return Mono that completes when the event has been sent
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
     * Handles evaluation errors by publishing an error event.
     * 
     * <p>Creates a SubmissionEvaluatedEvent with ERROR status and publishes it to notify
     * consumers of the failed evaluation. The error message is included in the feedback.</p>
     * 
     * @param event the original submission event that failed
     * @param error the exception that occurred during evaluation
     * @return Mono that completes when the error event has been published
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

        return publishEvaluatedEvent(errorEvent);
    }
}