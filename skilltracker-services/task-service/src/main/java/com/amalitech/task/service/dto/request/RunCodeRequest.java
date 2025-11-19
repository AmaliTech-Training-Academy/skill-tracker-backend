package com.amalitech.task.service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RunCodeRequest(
        @NotNull(message = "Task ID is required")
        UUID taskId,

        @NotBlank(message = "Code cannot be empty")
        String code,

        @NotNull(message = "Language ID is required")
        Integer languageId
) { }