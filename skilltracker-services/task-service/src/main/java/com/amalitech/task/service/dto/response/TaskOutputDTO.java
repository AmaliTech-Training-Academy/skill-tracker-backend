package com.amalitech.task.service.dto.response;

import lombok.*;

import java.util.UUID;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaskOutputDTO {
    private UUID userId;
    private String summary;
    private String current_skill;
    private String recommended_next_skill;
    private String reasoning;
    private String difficulty;
}