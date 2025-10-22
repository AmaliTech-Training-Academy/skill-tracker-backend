package com.amalitech.task.service.model.feedback.impl;

import com.amalitech.task.service.model.feedback.SubmissionFeedback;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodingSubmissionFeedback implements SubmissionFeedback {
    private boolean allPassed;
    private int testCasesPassed;
    private int testCasesTotal;
    private List<TestCaseResult> testCaseResults;
    private String lintingReport;
    private String stdout;
    private String stderr;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestCaseResult {
        private UUID testCaseId; // Link to TaskTestCase
        private boolean isHidden;
        private boolean passed;
        private String expected;
        private String actual;
        private long executionTimeMs;
    }
}
