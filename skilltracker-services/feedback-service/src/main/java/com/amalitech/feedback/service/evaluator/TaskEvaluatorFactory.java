package com.amalitech.feedback.service.evaluator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory for selecting the appropriate task evaluator based on task type.
 * Automatically discovers all TaskEvaluator implementations via Spring.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TaskEvaluatorFactory {

    private final Map<String, TaskEvaluator> evaluators;

    /**
     * Constructor that auto-wires all TaskEvaluator beans and creates a lookup map.
     */
    public TaskEvaluatorFactory(List<TaskEvaluator> evaluatorList) {
        this.evaluators = evaluatorList.stream()
                .collect(Collectors.toMap(
                        TaskEvaluator::getTaskType,
                        Function.identity()
                ));
        
        log.info("Registered task evaluators: {}", evaluators.keySet());
    }

    /**
     * Gets the appropriate evaluator for the given task type.
     * 
     * @param taskType The task type (e.g., "CODING", "MCQ", "ESSAY")
     * @return The evaluator for the task type
     * @throws IllegalArgumentException if no evaluator is found for the task type
     */
    public TaskEvaluator getEvaluator(String taskType) {
        TaskEvaluator evaluator = evaluators.get(taskType);
        
        if (evaluator == null) {
            log.error("No evaluator found for task type: {}", taskType);
            throw new IllegalArgumentException("Unsupported task type: " + taskType);
        }
        
        return evaluator;
    }
}
