package com.amalitech.analytics.service.repository;

import com.amalitech.analytics.service.model.UserSkillSelection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserSkillSelectionRepository extends JpaRepository<UserSkillSelection, UUID> {
    UserSkillSelection findByUserIdAndSkillId(UUID userId, UUID skillId);
}
