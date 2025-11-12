package com.amalitech.task.service.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LearningPathDTO {
    private UUID userId;
    private String summary;
    private String current_skill;
    private String recommended_next_skill;
    private List<String> recommended_activities;
    private String reasoning;
    private String difficulty;
}
