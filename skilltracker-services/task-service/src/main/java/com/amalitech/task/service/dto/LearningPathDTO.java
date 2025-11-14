package com.amalitech.task.service.dto;

import lombok.*;

import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LearningPathDTO {
    private String userId;
    private String summary;
    private String current_skill;
    private String recommended_next_skill;
    private List<String> recommended_activities;
    private String reasoning;
    private String difficulty;
    private List<String> resources;
}
