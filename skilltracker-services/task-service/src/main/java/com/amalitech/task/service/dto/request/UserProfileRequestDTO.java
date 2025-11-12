package com.amalitech.task.service.dto.request;

import lombok.*;
import org.gentle.mcqgenerator.dto.CurrentProgressDTO;

import java.util.List;
import java.util.UUID;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileRequestDTO {
    private UUID userId;
    private List<String> career_goals;
    private CurrentProgressDTO current_progress;
}
