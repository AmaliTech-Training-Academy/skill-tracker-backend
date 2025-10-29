package com.amalitech.task.service.model;

import com.amalitech.task.service.model.content.TaskContent;
import com.amalitech.task.service.model.enums.TaskDifficulty;
import com.amalitech.task.service.model.enums.TaskType;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a task that learners must complete within the platform.
 * Each task can be of a specific type (MCQ, Essay, Coding, or Project)
 * and may contain structured JSON content describing the task details.
 *
 * For example:
 * - MCQ: question, options, and correct answer.
 * - Essay: topic, min/max words, and guidelines.
 * - Coding: prompt, examples, and constraints.
 *
 * The 'content' field is stored as JSON (using PostgreSQL JSONB).
 */
@Entity
@Table(name = "tasks",
        indexes = {
        @Index(name = "idx_task_difficulty_published", columnList = "difficulty, is_published"),
        })
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
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

    /**
     * Stores the actual task content (MCQ questions, essay prompts, coding instructions, etc.)
     * in JSON format. Allows flexibility for different task types.
     */
    @Type(JsonType.class)
    @Column(nullable = false, columnDefinition = "jsonb")
    private TaskContent content;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_definition_id", nullable = false)
    private TaskDefinition taskDefinition;

    @Column(nullable = false)
    private int version;

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
