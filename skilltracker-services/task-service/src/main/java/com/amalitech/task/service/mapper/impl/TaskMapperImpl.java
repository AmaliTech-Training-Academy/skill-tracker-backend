package com.amalitech.task.service.mapper.impl;

import com.amalitech.task.service.dto.TaskDTO;
import com.amalitech.task.service.mapper.TaskMapper;
import com.amalitech.task.service.model.Task;
import com.amalitech.task.service.model.TaskDefinition;
import org.springframework.stereotype.Component;

@Component
public class TaskMapperImpl implements TaskMapper {

    @Override
    public TaskDTO toDTO(Task task) {
        if (task == null) {
            return null;
        }

        String skillName = null;
        TaskDefinition definition = task.getTaskDefinition();
        if (definition != null && definition.getSkill() != null) {
            skillName = definition.getSkill().getName();
        }

        return TaskDTO.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .type(task.getType())
                .difficulty(task.getDifficulty())
                .content(task.getContent())
                .xpReward(task.getXpReward())
                .estimatedDuration(task.getEstimatedDurationInMinutes())
                .skillName(skillName)
                .version(task.getVersion())
                .build();
    }
}
