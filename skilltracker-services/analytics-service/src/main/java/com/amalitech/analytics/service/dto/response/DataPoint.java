package com.amalitech.analytics.service.dto.response;

import com.amalitech.analytics.service.model.enums.TaskType;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
public class DataPoint {
    private LocalDateTime timestamp;
    private Double score;
    private TaskType taskType;
    private Map<String, Double> rubricsScores;
}