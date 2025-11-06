package com.amalitech.task.service.dto.request;

import com.amalitech.task.service.dto.MCQquestionDTO;
import com.amalitech.task.service.dto.TaskDTO;
import com.amalitech.task.service.model.content.TaskContent;
import com.amalitech.task.service.model.enums.TaskDifficulty;
import com.amalitech.task.service.model.enums.TaskType;
import com.amalitech.user.service.model.User;
import lombok.*;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class McqRequestDTO {
    private UUID userId;
    private String interest;
    private String difficulty;
    private Integer no_of_questions;
}