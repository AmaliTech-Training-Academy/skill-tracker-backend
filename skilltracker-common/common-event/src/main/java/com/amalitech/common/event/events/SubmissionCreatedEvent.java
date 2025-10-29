package com.amalitech.common.event.events;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class SubmissionCreatedEvent {
    private UUID submissionId;
    private UUID userId;
    private UUID taskId;

    private String taskType;
    private String codeToEvaluate;
    private Integer languageId;
    private List<TestCaseData> testCases;

    private String essayToEvaluate;

    @Data
    @Builder
    public static class TestCaseData {
        private String input;
        private String expectedOutput;
    }
}