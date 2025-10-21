package com.amalitech.task.service.model;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "task_submissions",
        indexes = {
                @Index(name = "idx_submission_user_task", columnList = "user_id, task_id"),
                @Index(name = "idx_submission_status", columnList = "is_correct")
        })
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TaskSubmission {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(columnDefinition = "jsonb")
    @Type(JsonType.class)
    private Map<String, Object> answer;

    @Column(columnDefinition = "jsonb")
    @Type(JsonType.class)
    private Map<String, Object> feedback;

    private Boolean isCorrect;

    private Integer scoreEarned;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    @Column
    private LocalDateTime evaluatedAt;
}