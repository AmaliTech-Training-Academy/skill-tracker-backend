package com.amalitech.task.service.dto.request;

import com.amalitech.task.service.model.submission.SubmissionAnswer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SubmitAnswerRequest(
        @NotNull(message = "Task ID is required")
        UUID taskId,

        @NotNull(message = "Answer is required")
        @Valid
        SubmissionAnswer answer
) { }