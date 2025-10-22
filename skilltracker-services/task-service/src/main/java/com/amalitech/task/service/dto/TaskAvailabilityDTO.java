package com.amalitech.task.service.dto;

import com.amalitech.task.service.model.enums.TaskDifficulty;
import lombok.Builder;

@Builder
public record TaskAvailabilityDTO(
        String skillName,
        TaskDifficulty difficulty,
        Integer availableTaskCount,
        Boolean needsGeneration
) { }
