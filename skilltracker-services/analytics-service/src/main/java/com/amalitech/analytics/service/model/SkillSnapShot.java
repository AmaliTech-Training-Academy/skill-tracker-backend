package com.amalitech.analytics.service.model;


import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JPA entity representing a read-only snapshot of a skill's configuration.
 * <p>
 * Synchronized from an external skill service via events. Used by analytics to
 * determine XP thresholds and level boundaries.
 * </p>
 */
@Entity
@Table(name = "skill_snapshots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillSnapShot {

    @Id
    private UUID id;

    @Column(nullable = false, updatable = false)
    private String name;

    @Column(nullable = false, updatable = false)
    private String category;

    /**
     * JSONB map of level → XP threshold (e.g., "INTERMEDIATE" → 1500L).
     * <p>
     * Stored as PostgreSQL {@code jsonb} column.
     * </p>
     */
    @Type(JsonType.class)
    @Column(name = "level_xp_map", nullable = false, columnDefinition = "jsonb")
    private Map<String, Long> levelXpMap = new HashMap<>();

    /** Timestamp of last synchronization from skill service. */
    @Column(nullable = false)
    private LocalDateTime lastSyncedAt;
}

