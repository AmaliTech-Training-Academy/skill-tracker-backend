package com.amalitech.task.service.mapper;

import com.amalitech.task.service.dto.TaskDTO;
import com.amalitech.task.service.dto.response.AdminTaskDetailResponse;
import com.amalitech.task.service.dto.response.AdminTaskSummaryResponse;
import com.amalitech.task.service.model.Task;

public interface TaskMapper {

    /**
     * Maps a Task entity to a learner-facing TaskDTO.
     */
    TaskDTO toDTO(Task task);

    /**
     * Maps a Task entity to a lightweight admin summary DTO.
     */
    AdminTaskSummaryResponse toAdminSummaryDTO(Task task);


    /**
     * Maps a Task entity to a full-detail admin DTO.
     */
    AdminTaskDetailResponse toAdminDetailDTO(Task task);
    }