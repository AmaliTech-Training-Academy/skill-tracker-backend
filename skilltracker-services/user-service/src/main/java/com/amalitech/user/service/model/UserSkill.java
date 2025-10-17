package com.amalitech.user.service.model;

import com.amalitech.user.service.model.enums.DifficultyLevel;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
<<<<<<< HEAD
 * Represents the association between a {@link User} and a {@link Skill}.
 * <p>
 * This entity serves as a junction (join) table for the many-to-many relationship
 * between users and skills, mapping which skills are associated with which users.
 * It is mapped to the database table named "user_skills".
 * <p>
 * Each instance links a single user to a single skill, and may contain additional
 * metadata about the association (such as when the skill was selected).
=======
 * Represents the association between a user and a skill, tracking their progress.
 * This entity links users to skills they are learning or have mastered,
 * storing their current level and accumulated experience points (XP).
 *
 * <p>It is mapped to the database table named "user_skills".
 *
 * <p>This is a junction entity that creates a many-to-many relationship
 * between {@link User} and {@link Skill}, with additional progress tracking fields.
 *
 * <h2>XP and Level Progression System</h2>
 * <p>Users accumulate XP by completing tasks. When their total XP meets or exceeds
 * the threshold for a higher difficulty level, they are automatically promoted.
 * The XP thresholds are defined in the associated {@link Skill}'s levelXpMap.
 *
 * <p><b>Example XP Progression:</b>
 * <pre>
 * levelXpMap = {
 *   "BEGINNER": 0,
 *   "INTERMEDIATE": 1000,
 *   "ADVANCED": 3000
 * }
 *
 * User starts: currentLevel = BEGINNER, totalXp = 0
 * After task:  totalXp = 500  → still BEGINNER
 * After task:  totalXp = 1200 → promoted to INTERMEDIATE
 * After task:  totalXp = 3500 → promoted to ADVANCED
 * </pre>
 *
 * @see User
 * @see Skill
 * @see DifficultyLevel
>>>>>>> f670043 (feat(entities): add User and UserSkill with XP-based progression)
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

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "current_level", nullable = false)
    private DifficultyLevel currentLevel = DifficultyLevel.BEGINNER;

    @Min(0)
    @Column(name = "total_xp", nullable = false)
    private Long totalXp = 0L;

    @CreationTimestamp
    @Column(name = "selected_at", nullable = false, updatable = false)
    private LocalDateTime selectedAt;


    /**
     * Validates that the user's current level is consistent with their total XP.
     * Ensures that the user has earned at least the minimum XP required for their current level.
     *
     * <p>This validation runs automatically before persisting or updating the entity.
     *
     * @throws IllegalStateException if totalXp is less than the required XP for currentLevel
     */
    @PrePersist
    @PreUpdate
    private void validateLevelAndXp() {
        if (skill != null && skill.getLevelXpMap() != null) {
            Long requiredXp = skill.getLevelXpMap().get(currentLevel.name());

            if (requiredXp != null && totalXp < requiredXp) {
                throw new IllegalStateException(
                        String.format("XP (%d) is less than required for level %s (%d)",
                                totalXp, currentLevel, requiredXp)
                );
            }
        }
    }

    /**
     * Adds experience points to the user's total and automatically checks for tier promotion.
     * If the user's new total XP qualifies them for a higher difficulty level, they are
     * automatically promoted to that level.
     *
     * <p>This method is transactional-safe and can be called multiple times. The promotion
     * logic ensures that users are always at the highest tier they qualify for based on
     * their total XP.
     *
     * <h3>Promotion Behavior:</h3>
     * <ul>
     *   <li>Users can skip tiers if they earn enough XP in one task</li>
     *   <li>Promotion is immediate and automatic</li>
     *   <li>There is no demotion - once promoted, the level cannot decrease</li>
     *   <li>Multiple calls with small XP amounts will promote when threshold is reached</li>
     * </ul>
     *
     * @param xpToAdd the amount of XP to add, must be positive
     * @return {@code true} if the user was promoted to a higher difficulty level,
     *         {@code false} if their level remained the same or if xpToAdd was invalid
     * @see #checkForTierPromotion()
     */
    public boolean addXp(long xpToAdd) {
        if (xpToAdd <= 0) {
            return false;
        }
        this.totalXp += xpToAdd;
        return checkForTierPromotion();
    }

    /**
     * Checks if the user qualifies for a higher difficulty tier based on their current total XP.
     * If qualified, automatically promotes the user to the highest tier they can reach.
     *
     * <p>This method iterates through all difficulty levels defined in {@link DifficultyLevel}
     * and compares the user's total XP against the thresholds defined in the skill's levelXpMap.
     * The user is promoted to the highest tier for which they meet the XP requirement.
     *
     * <h3>Algorithm:</h3>
     * <ol>
     *   <li>Retrieve the XP threshold map from the associated skill</li>
     *   <li>Iterate through all DifficultyLevel values (BEGINNER, INTERMEDIATE, ADVANCED)</li>
     *   <li>For each tier, check if:
     *     <ul>
     *       <li>The tier has a defined XP threshold</li>
     *       <li>User's totalXp meets or exceeds the threshold</li>
     *       <li>The tier is higher than the current highest qualifying tier (using ordinal comparison)</li>
     *     </ul>
     *   </li>
     *   <li>If a higher tier is found, update currentLevel and return true</li>
     *   <li>If no higher tier is found, return false</li>
     * </ol>
     *
     * <h3>Why ordinal() comparison?</h3>
     * <p>The method uses {@code tier.ordinal() > highestAchievedTier.ordinal()} to ensure
     * that the enum declaration order defines the tier hierarchy. This makes the logic
     * robust and independent of the XP threshold values.
     *
     * <pre>
     * enum DifficultyLevel {
     *   BEGINNER,      // ordinal = 0
     *   INTERMEDIATE,  // ordinal = 1
     *   ADVANCED       // ordinal = 2
     * }
     * </pre>
     *
     * <h3>Edge Cases:</h3>
     * <ul>
     *   <li>If skill or levelXpMap is null, returns false (no promotion possible)</li>
     *   <li>If user already at highest tier, returns false</li>
     *   <li>If XP map is incomplete, only checks available tiers</li>
     * </ul>
     *
     * @return {@code true} if the user's currentLevel was updated to a higher tier,
     *         {@code false} if the level remained unchanged
     */
    private boolean checkForTierPromotion() {
        if (this.skill == null || this.skill.getLevelXpMap() == null) {
            return false;
        }

        Map<String, Long> thresholdMap = this.skill.getLevelXpMap();
        DifficultyLevel highestAchievedTier = this.currentLevel;

        for (DifficultyLevel tier : DifficultyLevel.values()) {
            Long requiredXp = thresholdMap.get(tier.name());

            if (requiredXp != null && this.totalXp >= requiredXp && tier.ordinal() > highestAchievedTier.ordinal()) {
                highestAchievedTier = tier;
            }
        }

        if (highestAchievedTier != this.currentLevel) {
            this.currentLevel = highestAchievedTier;
            return true;
        }

        return false;
    }
}
