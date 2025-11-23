package com.amalitech.analytics.service.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "user_skill_selection")
@Getter
@Setter
@ToString(exclude = "event")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserSkillSelection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID skillId;

    private String selectedLevel;

    private String skillName;

    @Column(nullable = false, updatable = false)
    private String initialClaimLevel;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_skill_supported_task_types",
            joinColumns = @JoinColumn(name = "user_skill_selection_id"))
    @Column(name = "task_type")
    private Set<String> supportedTaskTypes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private UserSkillEvent event;
}