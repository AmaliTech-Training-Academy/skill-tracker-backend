package com.amalitech.analytics.service.repository;

import com.amalitech.analytics.service.model.UserGoal;
import com.amalitech.analytics.service.model.enums.GoalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserGoalRepository extends JpaRepository<UserGoal, UUID> {

    /**
     * Finds all active goals for a specific user and skill.
     * Used by AnalyticsService to update progress.
     */
    List<UserGoal> findByUserIdAndSkillIdAndStatus(UUID userId, UUID skillId, GoalStatus status);

    /**
     * Finds all goals for a user by status (e.g., all ACTIVE goals for the dashboard).
     */
    List<UserGoal> findByUserIdAndStatus(UUID userId, GoalStatus status);

    /**
     * Finds all goals for a user for the main CRUD interface.
     */
    List<UserGoal> findByUserId(UUID userId);

    /**
     * Finds a specific goal owned by a specific user for secure access.
     */
    Optional<UserGoal> findByIdAndUserId(UUID id, UUID userId);
}
