package com.amalitech.task.service.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Table(name = "userLP")
@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserLearningPath {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(unique = true, nullable = false)
    private UUID id;

    private String userId;

    @Column(columnDefinition = "TEXT")
    private String summary;

    private String currentSkill;

    private String recommended_next_skill;

    @Column(columnDefinition = "TEXT")
    private List<String> recommended_activities;

    @Column(columnDefinition = "TEXT")
    private String reasoning;

    private String difficulty;

    @Column(columnDefinition = "TEXT")
    private List<String> resources;

}
