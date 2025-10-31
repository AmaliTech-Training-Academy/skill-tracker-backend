package com.amalitech.user.service.model;

import com.amalitech.user.service.model.enums.DifficultyLevel;
import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.*;

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

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "icon_url", length = 1024)
    private String iconUrl;

    /**
     * Defines the types of tasks this skill supports.
     * e.g., ["MCQ", "ESSAY", "CODING", "VERBAL"]
     * This is critical for the TaskGenerationService.
     */
    @Type(ListArrayType.class)
    @Column(name = "supported_task_types", columnDefinition = "text[]")
    private List<String> supportedTaskTypes = new ArrayList<>();

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
            throw new IllegalStateException("Level XP map must contain at least one difficulty level with XP threshold");
        }

        for (Long xp : levelXpMap.values()) {
            if (xp < 0) {
                throw new IllegalStateException("XP values cannot be negative");
            }
        }
    }
}
