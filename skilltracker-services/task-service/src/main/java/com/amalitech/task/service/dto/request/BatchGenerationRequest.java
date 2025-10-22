package com.amalitech.task.service.dto.request;

import com.amalitech.task.service.model.enums.TaskDifficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchGenerationRequest implements Serializable {
    private String skillName;
    private TaskDifficulty difficulty;
    private Integer requiredCount;
}