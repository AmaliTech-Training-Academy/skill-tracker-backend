package com.amalitech.task.service.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

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
