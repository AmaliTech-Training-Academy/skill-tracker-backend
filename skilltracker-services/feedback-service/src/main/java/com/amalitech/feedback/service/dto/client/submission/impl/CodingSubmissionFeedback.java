package com.amalitech.feedback.service.dto.client.submission.impl;

import com.amalitech.feedback.service.dto.client.submission.SubmissionFeedback;
import lombok.Builder;
import lombok.Data;

/**
 * The final "graded paper" we will build and send back.
 * (Copied from task-service)
 */
@Data
@Builder
public class CodingSubmissionFeedback implements SubmissionFeedback {
    private int passedTests;
    private int totalTests;
    private double executionTimeMs;
    private int memoryUsedKb;
    private String statusDescription; // e.g., "Accepted", "Wrong Answer"

    private String correctnessFeedback;
    private String efficiencyFeedback;
    private String styleFeedback;
    private String overallSuggestion;
    private String aiModelUsed;
}
