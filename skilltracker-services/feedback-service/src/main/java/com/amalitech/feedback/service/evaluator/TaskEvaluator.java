package com.amalitech.feedback.service.evaluator;

import com.amalitech.common.event.events.SubmissionCreatedEvent;
import com.amalitech.common.event.events.SubmissionEvaluatedEvent;
import reactor.core.publisher.Mono;

/**
 * Strategy interface for evaluating different task types.
 * Each task type (CODING, MCQ, ESSAY) will have its own implementation.
 */
public interface TaskEvaluator {
    
    /**
     * Evaluates a submission and returns the evaluation result.
     * 
     * @param event The submission event containing task details and user answer
     * @return A Mono containing the evaluated submission result
     */
    Mono<SubmissionEvaluatedEvent> evaluate(SubmissionCreatedEvent event);
    
    /**
     * Returns the task type this evaluator handles.
     * 
     * @return The task type (e.g., "CODING", "MCQ", "ESSAY")
     */
    String getTaskType();
}
