package com.amalitech.task.service.dto;

import com.amalitech.task.service.model.enums.SubmissionStatus;
import com.amalitech.task.service.model.feedback.SubmissionFeedback;
import com.amalitech.task.service.model.submission.SubmissionAnswer;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for a TaskSubmission.
 * This is the safe, flat representation of a submission.
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

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime submittedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime evaluatedAt;
}