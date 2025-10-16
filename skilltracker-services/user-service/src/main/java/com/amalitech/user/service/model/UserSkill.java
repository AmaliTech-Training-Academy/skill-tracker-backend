package com.amalitech.user.service.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a user in the system.
 * This entity is typically the core subject of the user-related services,
 * holding primary identification and essential details.
 *
 * <p>It is mapped to the database table named "users".
 *
 * <p>In the context of the 'user-skills' model, this entity is the 'many' side
 * of the one-to-many relationship with {@code UserSkill}, and the 'one' side
 * of the many-to-one relationship from {@code UserSkill}.
 *
 */
@Entity
@Table(name = "user_skills", indexes = {
        @Index(name = "idx_user_skill", columnList = "user_id, skill_id", unique = true),
        @Index(name = "idx_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserSkill {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @CreationTimestamp
    @Column(name = "selected_at", nullable = false, updatable = false)
    private LocalDateTime selectedAt;
}
