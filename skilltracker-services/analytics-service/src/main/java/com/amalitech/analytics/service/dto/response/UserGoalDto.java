package com.amalitech.analytics.service.dto.response;

import lombok.Data;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class UserGoalDto {
    private UUID skillId;
    private String goalDescription;
    private Double targetScore;
    private LocalDate targetDate;
}
