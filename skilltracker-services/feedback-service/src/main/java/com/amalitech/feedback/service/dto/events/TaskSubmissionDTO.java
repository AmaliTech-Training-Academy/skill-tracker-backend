package com.amalitech.feedback.service.dto.events;

import com.amalitech.feedback.service.dto.client.submission.SubmissionAnswer;
import com.amalitech.feedback.service.dto.client.submission.SubmissionFeedback;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * INCOMING DTO from RabbitMQ (from task-service).
 * This represents the "job" we need to do.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskSubmissionDTO {
    private UUID id;
    private UUID userId;
    private UUID taskId;
    private SubmissionStatus status;
    private SubmissionAnswer answer;
    private SubmissionFeedback feedback;
    private Boolean isCorrect;
    private Integer scoreEarned;
    private LocalDateTime submittedAt;
    private LocalDateTime evaluatedAt;

    public enum SubmissionStatus {
        PENDING, RUNNING, COMPLETED, ERROR
    }
}