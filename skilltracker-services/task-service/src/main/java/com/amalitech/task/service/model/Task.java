package com.amalitech.task.service.model;

import com.amalitech.task.service.model.enums.TaskDifficulty;
import com.amalitech.task.service.model.enums.TaskType;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "tasks",
        indexes = {
        @Index(name = "idx_task_difficulty_published", columnList = "difficulty, is_published"),
        @Index(name = "idx_task_skill", columnList = "skill_id")
        })
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskDifficulty difficulty;

    @Column(name = "skill_id", nullable = false)
    private UUID skillId;

    @Column(nullable = false, columnDefinition = "jsonb")
    @Type(JsonType.class)
    private Map<String, Object> content;

    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationInMinutes;

    @Column(nullable = false)
    private Integer xpReward = 0;

    @Column(name = "is_published", nullable = false)
    private Boolean isPublished = false;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
