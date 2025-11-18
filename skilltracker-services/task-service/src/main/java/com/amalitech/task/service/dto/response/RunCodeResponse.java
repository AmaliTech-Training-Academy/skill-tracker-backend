package com.amalitech.task.service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RunCodeResponse {
    private List<TestResultData> testResults;
    private boolean allTestsPassed;
    private int testsPassed;
    private int testsTotal;
    private String stdout;
    private String stderr;
    private Double avgExecutionTimeMs;
    private Integer avgMemoryUsedKb;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestResultData {
        private boolean passed;
        private String input;
        private String expectedOutput;
        private String actualOutput;
        private Long executionTimeMs;
        private Integer memoryUsedKb;
        private String statusDescription;
    }
}
