package com.amalitech.task.service.dto.response;

import com.amalitech.task.service.dto.LearningPathDTO;
import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LearningPathResponseDTO {
    private LearningPathDTO learningPath;
}
