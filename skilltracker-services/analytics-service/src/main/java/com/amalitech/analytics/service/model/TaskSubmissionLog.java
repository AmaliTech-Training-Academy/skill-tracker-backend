package com.amalitech.analytics.service.model;

import com.amalitech.analytics.service.model.enums.TaskType;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "task_submission_logs",

        indexes = {
                @Index(name = "idx_tasklog_user", columnList = "userId"),
                @Index(name = "idx_tasklog_skill", columnList = "skillId")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskSubmissionLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false, updatable = false)
    private UUID skillId;

    @Column(updatable = false)
    private String taskId;

    @Column(updatable = false)
    private int totalXpEarned;

    @Column(updatable = false)
    private TaskType taskType;


    @Column(updatable = false)
    private Boolean passed;

    @Type(JsonType.class)
    @Column(name = "rubrics", columnDefinition = "jsonb")
    private Map<String, Integer> rubricsScores = new HashMap<>();

    @Column(nullable = false, updatable = false)
    private Instant completedAt;

}