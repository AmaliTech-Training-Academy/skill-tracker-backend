package com.amalitech.task.service.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "task_test_cases",
        indexes = @Index(name = "idx_task_test_case_task", columnList = "task_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskTestCase {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(nullable = false)
    private String input;

    @Column(name = "expected_output", nullable = false)
    private String expectedOutput;

    @Column(name = "is_hidden", nullable = false)
    private Boolean isHidden = false;

    @Column(nullable = false)
    private Integer weight = 1;

    @Column(name = "timeout_ms", nullable = false)
    private Integer timeoutMs = 5000;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createdAt;
}
