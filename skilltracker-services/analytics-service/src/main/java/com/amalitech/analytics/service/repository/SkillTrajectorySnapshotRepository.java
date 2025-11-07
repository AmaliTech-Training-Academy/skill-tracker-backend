package com.amalitech.analytics.service.repository;

import com.amalitech.analytics.service.model.SkillTrajectorySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SkillTrajectorySnapshotRepository extends JpaRepository<SkillTrajectorySnapshot, UUID> {

    // Original method for specific day lookup (keep this)
    Optional<SkillTrajectorySnapshot> findByUserIdAndSkillIdAndSnapshotDate(UUID userId, UUID skillId, LocalDate snapshotDate);

}