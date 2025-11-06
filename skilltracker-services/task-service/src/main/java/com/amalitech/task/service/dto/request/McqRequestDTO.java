package com.amalitech.task.service.dto.request;

import com.amalitech.task.service.dto.MCQquestionDTO;
import com.amalitech.task.service.dto.TaskDTO;
import com.amalitech.task.service.model.content.TaskContent;
import com.amalitech.task.service.model.enums.TaskDifficulty;
import com.amalitech.task.service.model.enums.TaskType;
import com.amalitech.user.service.model.User;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class McqRequestDTO {
    private String title;
    private String description;
    private TaskType type;
    private TaskDifficulty difficulty;
    private String content;
    private Integer xpReward;
    private String interest;
    private String difficulty_level;
    private Integer no_of_questions;
}