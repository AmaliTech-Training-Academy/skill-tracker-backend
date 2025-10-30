package com.amalitech.task.service.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity representing the pre-written boilerplate or starter code provided to a user
 * for a specific programming task in a particular language.
 * <p>
 * This entity is crucial for coding challenges, as it provides the function signature,
 * necessary imports, or basic structure required for the user to begin solving the problem.
 * The unique constraint ensures that only one set of starter code exists for a given
 * task and programming language combination.
 */
@Entity
@Table(name = "task_starter_code",
        uniqueConstraints = @UniqueConstraint(columnNames = {"task_id", "language_id"}),
        indexes = @Index(name = "idx_task_starter_code_task", columnList = "task_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskStarterCode {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "language_id", nullable = false)
    private ProgrammingLanguage programmingLanguage;

    @Lob
    @Column(nullable = false)
    private String code;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createdAt;
}