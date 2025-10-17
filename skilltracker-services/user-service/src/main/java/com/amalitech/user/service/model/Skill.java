package com.amalitech.user.service.model;

import com.amalitech.user.service.model.enums.DifficultyLevel;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.UUID;
import java.util.Map;

/**
 * Represents a defined skill within the system that users can possess.
 * This entity stores metadata about a skill, such as its name, description,
 * categorization, and difficulty level.
 *
 * <p>It is mapped to the database table named "skills".
 *
 * <p>This entity is the 'many' side in a many-to-one relationship with {@link UserSkill},
 * linking it to users who possess it. The unique index on the name ensures that
 * no two skills have the same name.
 *
 * @see UserSkill
 * @see DifficultyLevel
 */
@Entity
@Table(name = "skills", indexes = {
        @Index(name = "idx_name", columnList = "name", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Skill {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_level", nullable = false)
    private DifficultyLevel difficultyLevel;

    @Type(JsonType.class)
    @Column(name = "level_xp_map", nullable = false, columnDefinition = "jsonb")
    private Map<String, Long> levelXpMap = new HashMap<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    private void validateLevelXpMap() {
        if (levelXpMap == null || levelXpMap.isEmpty()) {
            throw new IllegalStateException("Level XP map cannot be empty");
        }

        for (Long xp : levelXpMap.values()) {
            if (xp < 0) {
                throw new IllegalStateException("XP values cannot be negative");
            }
        }
    }
}

