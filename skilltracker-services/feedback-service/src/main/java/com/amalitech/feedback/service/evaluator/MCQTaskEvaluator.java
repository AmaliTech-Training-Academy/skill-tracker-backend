package com.amalitech.feedback.service.evaluator;

import com.amalitech.common.event.events.SubmissionCreatedEvent;
import com.amalitech.common.event.events.SubmissionEvaluatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Evaluator for MCQ (Multiple Choice Question) tasks.
 * TODO: Implement MCQ evaluation logic.
 */
@Component
@Slf4j
public class MCQTaskEvaluator implements TaskEvaluator {

    @Override
    public Mono<SubmissionEvaluatedEvent> evaluate(SubmissionCreatedEvent event) {
        log.warn("MCQ evaluation not yet implemented for submission: {}", event.getSubmissionId());
        
        return Mono.just(SubmissionEvaluatedEvent.builder()
                .submissionId(event.getSubmissionId())
                .userId(event.getUserId())
                .status("ERROR")
                .score(0)
                .isCorrect(false)
                .feedbackType("MCQ")
                .overallFeedback("MCQ evaluation is not yet implemented.")
                .build());
    }

    @Override
    public String getTaskType() {
        return "MCQ";
    }
}
