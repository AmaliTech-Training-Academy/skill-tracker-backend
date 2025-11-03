package com.amalitech.task.service.model;

import com.amalitech.task.service.model.view.SkillView;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity representing the foundational definition or metadata for a type of task
 * within the SkillBoost platform.
 * <p>
 * This entity stores immutable, high-level attributes of a task, acting as a container
 * for all subsequent content versions (not shown here). It establishes a mandatory
 * link to a {@link SkillView} and tracks the current, highest version number of the task content.
 * It is essential for managing the lifecycle and categorization of all AI-generated challenges.
 */
@Entity
@Table(name = "task_definitions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskDefinition {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id", nullable = false)
    private SkillView skill;

    private String title;

    @Column(name = "latest_version")
    private int latestVersion = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}