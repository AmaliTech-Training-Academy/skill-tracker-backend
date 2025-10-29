package com.amalitech.task.service.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity representing a prerequisite relationship between two tasks within the SkillBoost platform.
 * <p>
 * This entity defines a directed dependency: the task identified by {@code task_id}
 * requires the task identified by {@code prerequisite_task_id} to be completed first.
 * It ensures the platform can enforce a structured, sequential learning path. The unique
 * constraint prevents redundant prerequisite entries between the same two tasks.
 */
@Entity
@Table(name = "task_prerequisites",
        uniqueConstraints = @UniqueConstraint(columnNames = {"task_id", "prerequisite_task_id"}),
        indexes = @Index(name = "idx_task_prereq_task", columnList = "task_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskPrerequisite {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(name = "prerequisite_task_id", nullable = false)
    private UUID prerequisiteTaskId;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createdAt;
}