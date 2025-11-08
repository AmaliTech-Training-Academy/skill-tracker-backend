package com.amalitech.task.service.model;

import com.amalitech.task.service.model.enums.TaskDifficulty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * A local, read-only replica of a user's skill and difficulty level.
 * This data is populated by events from the user-service (e.g., UserOnboardingCompletedEvent)
 * and is used by the TaskService to quickly fetch personalized tasks without
 * making a cross-service call.
 */
@Entity
@Table(name = "user_skill_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSkillProfile {

    @EmbeddedId
    private UserSkillId id;

    @Column(nullable = false)
    private String skillName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskDifficulty difficulty;

    /**
     * Composite key for the user-skill relationship.
     */
    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSkillId implements Serializable {
        @Column(name = "user_id")
        private UUID userId;

        @Column(name = "skill_id")
        private UUID skillId;
    }
}