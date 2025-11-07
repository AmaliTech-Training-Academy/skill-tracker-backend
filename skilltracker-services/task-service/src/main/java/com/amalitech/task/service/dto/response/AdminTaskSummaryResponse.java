package com.amalitech.task.service.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.UUID;

public record AdminTaskSummaryResponse(
        UUID taskId,
        String title,
        String type,
        String difficulty,
        int version,
        boolean isPublished,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {}