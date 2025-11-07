package com.amalitech.analytics.service.repository;

import com.amalitech.analytics.service.model.SkillSnapShot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface SkillSnapshotRepository extends JpaRepository<SkillSnapShot, UUID> {

}
