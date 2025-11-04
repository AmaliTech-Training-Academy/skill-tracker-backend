package com.amalitech.task.service.dto.request;

import com.amalitech.task.service.dto.TaskDTO;
import com.amalitech.user.service.model.User;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class McqRequestDTO {
    private String instruction_type;
    private User user_details;
    private TaskDTO task_details;
    private List<String> constraints;
    private int no_of_questions;
}