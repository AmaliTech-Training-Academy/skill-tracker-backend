package com.amalitech.analytics.service.model;

import com.amalitech.analytics.service.dto.UserRubricStatsId;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "user_rubric_stats",
        indexes = {
                @Index(name = "idx_user_rubrics_stats_user_id", columnList = "user_id, rubric"),
        }
)
@IdClass(UserRubricStatsId.class)
@Getter
@Setter
@NoArgsConstructor
public class UserRubricStats {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Id
    private String rubric;

    private long totalScore = 0L;
    private long totalMaxScore = 0L;
    private int submissionCount = 0;

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    public void addSubmission(int score, int maxScore) {
        this.totalScore += score;
        this.totalMaxScore += maxScore;
        this.submissionCount++;
        this.updatedAt = Instant.now();
    }

    public double getAveragePercentage() {
        if (totalMaxScore == 0) return 100.0;
        return (double) totalScore / totalMaxScore * 100.0;
    }
}
