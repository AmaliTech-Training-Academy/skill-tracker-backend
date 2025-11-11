package com.amalitech.task.service.dto.response;

import com.amalitech.task.service.dto.TaskDTO;
import lombok.Builder;
import org.springframework.data.domain.Page;

@Builder
public record UserTasksResponse(
        Page<TaskDTO> pending,
        Page<TaskDTO> completed
) {}