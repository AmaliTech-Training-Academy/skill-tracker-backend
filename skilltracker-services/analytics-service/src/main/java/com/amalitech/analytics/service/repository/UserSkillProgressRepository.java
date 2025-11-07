package com.amalitech.analytics.service.repository;

import com.amalitech.analytics.service.model.UserSkillProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSkillProgressRepository extends JpaRepository<UserSkillProgress, UUID> {

    // Optimized for the Write Path (checking/updating progress on event)
    Optional<UserSkillProgress> findByUserIdAndSkillId(UUID userId, UUID skillId);

    // Optimized for the Read Path (fetching all skills for the main dashboard)
    List<UserSkillProgress> findAllByUserId(UUID userId);

    // Needed by the GlobalPercentileCalculator to iterate over skills
    @Query("SELECT DISTINCT p.skillId FROM UserSkillProgress p")
    List<UUID> findAllDistinctSkillIds();
}