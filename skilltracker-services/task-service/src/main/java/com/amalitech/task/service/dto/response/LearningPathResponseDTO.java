package com.amalitech.task.service.dto.response;

import lombok.*;
import org.gentle.mcqgenerator.dto.TaskOutputDTO;
import org.gentle.mcqgenerator.dto.request.UserProfileRequestDTO;

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
