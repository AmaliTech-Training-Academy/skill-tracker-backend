package com.amalitech.feedback.service.evaluator;

import com.amalitech.common.event.events.SubmissionCreatedEvent;
import com.amalitech.common.event.events.SubmissionEvaluatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Evaluator for ESSAY tasks.
 */
@Component
@Slf4j
public class EssayTaskEvaluator implements TaskEvaluator {

    @Override
    public Mono<SubmissionEvaluatedEvent> evaluate(SubmissionCreatedEvent event) {
        log.warn("ESSAY evaluation not yet implemented for submission: {}", event.getSubmissionId());
        
        return Mono.just(SubmissionEvaluatedEvent.builder()
                .submissionId(event.getSubmissionId())
                .userId(event.getUserId())
                .status("ERROR")
                .score(0)
                .isCorrect(false)
                .feedbackType("ESSAY")
                .overallFeedback("Essay evaluation is not yet implemented.")
                .build());
    }

    @Override
    public String getTaskType() {
        return "ESSAY";
    }
}
