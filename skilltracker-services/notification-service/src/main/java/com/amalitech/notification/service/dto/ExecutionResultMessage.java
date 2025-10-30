package com.amalitech.notification.service.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * DTO for sending real-time code execution results to the client.
 * This message is sent when a user's submission has been executed against test cases.
 */
@Data
@Builder
public class ExecutionResultMessage {
    
    private UUID submissionId;
    private String stdout;
    private String stderr;
    private List<TestResult> testResults;
    private boolean allTestsPassed;
    private int testsPassed;
    private int testsTotal;
    private Double avgExecutionTimeMs;
    private Integer avgMemoryUsedKb;

    /**
     * Represents the result of a single test case execution.
     */
    @Data
    @Builder
    public static class TestResult {
        private boolean passed;
        private String input;
        private String expectedOutput;
        private String actualOutput;
        private Long executionTimeMs;
        private Integer memoryUsedKb;
        private String statusDescription;
    }
}

