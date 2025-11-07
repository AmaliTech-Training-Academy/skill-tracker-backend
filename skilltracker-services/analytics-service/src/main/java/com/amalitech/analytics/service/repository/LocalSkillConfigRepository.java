package com.amalitech.analytics.service.repository;

import com.amalitech.analytics.service.model.LocalSkillConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LocalSkillConfigRepository extends JpaRepository<LocalSkillConfig, UUID> {
}
