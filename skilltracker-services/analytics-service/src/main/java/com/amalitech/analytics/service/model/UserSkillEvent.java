package com.amalitech.analytics.service.model;

import com.amalitech.analytics.service.model.enums.SkillEventSource;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_skill_event")
@Data
public class UserSkillEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID Id;

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SkillEventSource source;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.Set<UserSkillSelection> selections = new java.util.HashSet<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime occurredAt;
}
