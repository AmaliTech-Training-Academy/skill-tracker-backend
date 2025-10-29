package com.amalitech.notification.service.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

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
