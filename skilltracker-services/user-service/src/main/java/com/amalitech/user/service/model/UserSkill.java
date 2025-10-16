package com.amalitech.user.service.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents the association between a {@link User} and a {@link Skill}.
 * <p>
 * This entity serves as a junction (join) table for the many-to-many relationship
 * between users and skills, mapping which skills are associated with which users.
 * It is mapped to the database table named "user_skills".
 * <p>
 * Each instance links a single user to a single skill, and may contain additional
 * metadata about the association (such as when the skill was selected).
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
