package com.amalitech.task.service.dto.response;

import com.amalitech.task.service.dto.LearningPathDTO;
import com.amalitech.task.service.dto.request.UserProfileRequestDTO;
import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LearningPathResponseDTO {
    private LearningPathDTO learningPath;
}
