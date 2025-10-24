package com.amalitech.task.service.dto;

import com.amalitech.task.service.model.feedback.SubmissionFeedback;

import java.util.UUID;

public record SubmissionResultDTO(
        UUID submissionId,
        Boolean isCorrect,
        Integer scoreEarned,
        Integer totalXp,
        SubmissionFeedback feedback
) { }
