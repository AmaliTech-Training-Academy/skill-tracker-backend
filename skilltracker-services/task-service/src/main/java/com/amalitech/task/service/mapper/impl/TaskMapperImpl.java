package com.amalitech.task.service.mapper.impl;

import com.amalitech.task.service.dto.MCQquestionDTO;
import com.amalitech.task.service.dto.TaskDTO;
import com.amalitech.task.service.dto.response.AdminTaskDetailResponse;
import com.amalitech.task.service.dto.response.AdminTaskSummaryResponse;
import com.amalitech.task.service.mapper.TaskMapper;
import com.amalitech.task.service.model.Task;
import com.amalitech.task.service.model.TaskDefinition;
import com.amalitech.task.service.model.view.SkillView;
import com.amalitech.task.service.model.enums.TaskDifficulty;
import com.amalitech.task.service.model.enums.TaskType;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.time.LocalDateTime;

@Component
public class TaskMapperImpl implements TaskMapper {

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public AdminTaskSummaryResponse toAdminSummaryDTO(Task task) {
        if (task == null) {
            return null;
        }

        return new AdminTaskSummaryResponse(
                task.getId(),
                task.getTitle(),
                task.getType().name(),
                task.getDifficulty().name(),
                task.getVersion(),
                task.getIsPublished(),
                task.getCreatedAt()
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AdminTaskDetailResponse toAdminDetailDTO(Task task) {
        if (task == null) {
            return null;
        }

        TaskDefinition definition = task.getTaskDefinition();
        UUID taskDefId = null;
        String skillName = null;

        if (definition != null) {
            taskDefId = definition.getId();
            SkillView skill = definition.getSkill();
            if (skill != null) {
                skillName = skill.getName();
            }
        }

        return new AdminTaskDetailResponse(
                task.getId(),
                taskDefId,
                task.getTitle(),
                task.getDescription(),
                skillName,
                task.getType().name(),
                task.getDifficulty().name(),
                task.getContent(),
                task.getVersion(),
                task.getIsPublished(),
                task.getEstimatedDurationInMinutes(),
                task.getXpReward(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }

    public static Task toEntity(MCQquestionDTO dto) {
        if (dto == null) return null;
        Task task = new Task();
        task.setTitle(dto.getQuestion_title());
        task.setDescription(dto.getQuestion_description());
        task.setType(TaskType.valueOf(dto.getQuestion_type()));
        task.setDifficulty(TaskDifficulty.valueOf(dto.getQuestion_difficulty()));
//        task.setContent(dto.getContent()); // Come to this later...
        task.setTaskDefinition(TaskDefinition.builder().build());
        task.setEstimatedDurationInMinutes(dto.getQuestion_duration());
        task.setXpReward(dto.getXpReward());
        task.setUpdatedAt(LocalDateTime.now());

        return task;
    }
}