package com.amalitech.task.service.dto;

import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CurrentProgressDTO {
    private String skill;
    private String difficulty;
    private int performance_percentage;
}