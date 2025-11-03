package com.amalitech.analytics.service.model;

import com.amalitech.analytics.service.model.enums.TaskType;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "skill_progress")
@Data
@NoArgsConstructor
public class SkillProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID skillId;

    @Column(nullable = false)
    private UUID taskId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskType taskType; // +++ NEW FIELD: Written, Coding, MCQ +++

    @Type(JsonBinaryType.class) // Requires Hibernate/JPA JSON library (e.g., hibernate-types)
    @Column(columnDefinition = "jsonb")
    private Map<String, Double> rubricsScores; // +++ NEW FIELD: Stores granular scores +++

    @Column(nullable = false)
    private Double score;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    public SkillProgress(UUID userId, UUID skillId, UUID taskId, Double score,
                         TaskType taskType, Map<String, Double> rubricsScores) {
        this.userId = userId;
        this.skillId = skillId;
        this.taskId = taskId;
        this.score = score;
        this.taskType = taskType;
        this.rubricsScores = rubricsScores;
        this.timestamp = LocalDateTime.now();
    }
}
