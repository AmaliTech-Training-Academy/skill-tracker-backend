package com.amalitech.analytics.service.repository;

import com.amalitech.analytics.service.model.UserSkillEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserSkillEventRepository extends JpaRepository<UserSkillEvent, UUID> {
}
