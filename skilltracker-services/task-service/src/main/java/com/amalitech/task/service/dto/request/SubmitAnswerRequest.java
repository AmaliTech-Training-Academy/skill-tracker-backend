package com.amalitech.task.service.dto.request;

import com.amalitech.task.service.model.submission.SubmissionAnswer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * An immutable Data Transfer Object (DTO) used by the client to submit an answer
 * to a specific task for evaluation.
 * <p>
 * This request object is the critical input for the {@code SubmissionController}'s
 * POST endpoint. It bundles the unique identifier of the task being attempted and
 * the content of the user's answer, ensuring both are present and the answer structure
 * itself is valid before processing begins.
 *
 * @param taskId The unique identifier (UUID) of the specific challenge the user is attempting to answer. Must be present.
 * @param answer The encapsulated content of the user's response, which is a nested,
 * validated object of type {@link SubmissionAnswer} to accommodate various
 * task types (code, text, choices, etc.). Must be present and valid.
 */
public record SubmitAnswerRequest(
        @NotNull(message = "Task ID is required")
        UUID taskId,

        @NotNull(message = "Answer is required")
        @Valid
        SubmissionAnswer answer
) { }