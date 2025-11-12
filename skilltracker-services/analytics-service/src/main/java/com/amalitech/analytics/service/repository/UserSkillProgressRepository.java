package com.amalitech.analytics.service.repository;

import com.amalitech.analytics.service.model.UserSkillProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSkillProgressRepository extends JpaRepository<UserSkillProgress, UUID> {

    Optional<UserSkillProgress> findByUserIdAndSkillId(UUID userId, UUID skillId);

    List<UserSkillProgress> findAllByUserId(UUID userId);

    @Query("SELECT DISTINCT p.skillId FROM UserSkillProgress p")
    List<UUID> findAllDistinctSkillIds();
}