package com.amalitech.analytics.service.repository;

import com.amalitech.analytics.service.model.UserGoal;
import com.amalitech.analytics.service.model.enums.GoalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserGoalRepository extends JpaRepository<UserGoal, UUID> {
    List<UserGoal> findByUserIdAndStatus(UUID userId, GoalStatus status);
    List<UserGoal> findByUserIdAndSkillIdAndStatus(UUID userId, UUID skillId, GoalStatus status);
}
