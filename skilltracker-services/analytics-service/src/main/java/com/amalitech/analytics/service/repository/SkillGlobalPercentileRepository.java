package com.amalitech.analytics.service.repository;

import com.amalitech.analytics.service.model.SkillGlobalPercentile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SkillGlobalPercentileRepository extends JpaRepository<SkillGlobalPercentile, UUID> {
    // findById(UUID skillId) is inherited, serving the purpose of fetching the current rank data.
}