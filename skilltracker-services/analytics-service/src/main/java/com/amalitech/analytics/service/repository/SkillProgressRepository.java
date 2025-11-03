package com.amalitech.analytics.service.repository;

import com.amalitech.analytics.service.model.SkillProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SkillProgressRepository extends JpaRepository<SkillProgress, UUID> {
    List<SkillProgress> findByUserIdOrderByTimestampAsc(UUID userId);

    @Query("SELECT sp FROM SkillProgress sp WHERE sp.userId = :userId " +
            "AND (:skillId IS NULL OR sp.skillId = :skillId) " +
            "AND sp.timestamp BETWEEN :startDate AND :endDate " +
            "ORDER BY sp.timestamp ASC")
    List<SkillProgress> findUserData(UUID userId, Optional<UUID> skillId, LocalDateTime startDate, LocalDateTime endDate);
}
