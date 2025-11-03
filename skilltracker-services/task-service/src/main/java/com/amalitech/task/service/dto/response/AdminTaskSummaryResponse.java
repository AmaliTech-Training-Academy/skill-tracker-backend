package com.amalitech.task.service.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminTaskSummaryResponse(
        UUID taskId,
        String title,
        String type,
        String difficulty,
        int version,
        boolean isPublished,
        LocalDateTime createdAt
) {}