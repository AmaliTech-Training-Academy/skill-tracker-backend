package com.amalitech.analytics.service.model;


import com.amalitech.analytics.service.model.enums.GoalStatus;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "user_goals")
@Data
public class UserGoal {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false)
    private UUID userId;
    @Column(nullable = false)
    private UUID skillId;
    @Column(nullable = false)
    private String goalDescription;
    @Column(nullable = false)
    private Double targetScore;
    private LocalDate targetDate;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GoalStatus status = GoalStatus.ACTIVE;
}