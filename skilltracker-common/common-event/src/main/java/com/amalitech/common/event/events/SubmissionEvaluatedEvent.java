package com.amalitech.common.event.events;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;
import java.util.List;

@Data
@Builder
public class SubmissionEvaluatedEvent {
    private UUID submissionId;
    private UUID userId; // Pass this through for WebSocket routing
    private String status; // "COMPLETED", "ERROR"
    private int score;
    private boolean isCorrect;

    // --- Extracted, flat feedback ---
    private String feedbackType; // "CODING", "ESSAY", "MCQ"
    private String overallFeedback; // General AI suggestion
    private List<String> testCaseResults; // e.g., ["Test 1: PASSED", "Test 2: FAILED"]
    // ... add any other simple fields `task-service` needs to store
}