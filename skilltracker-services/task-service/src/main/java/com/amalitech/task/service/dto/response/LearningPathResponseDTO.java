package com.amalitech.task.service.dto.response;

import com.amalitech.task.service.dto.request.UserProfileRequestDTO;
import lombok.*;


@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LearningPathResponseDTO {
    private String type;
    private UserProfileRequestDTO userInput;
    private TaskOutputDTO TaskOutput;
    private String difficulty;
    private String next_skill;
    private String constraints;
}
