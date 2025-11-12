package com.amalitech.task.service.dto.request;

import com.amalitech.task.service.dto.CurrentProgressDTO;
import lombok.*;

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
