package com.amalitech.task.service.service;

import com.amalitech.task.service.dto.response.RunCodeResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Service for executing user code against task test cases.
 * Provides synchronous code execution with immediate test results.
 */
public interface CodeExecutionService {

    /**
     * Executes code against all test cases for a given task.
     *
     * @param taskId the ID of the task
     * @param code the source code to execute
     * @param languageId the Judge0 language ID
     * @return Mono containing the execution results with test metrics
     * @throws IllegalArgumentException if task not found, not a coding task, or has no test cases
     */
    Mono<RunCodeResponse> executeCode(UUID taskId, String code, Integer languageId);
}
