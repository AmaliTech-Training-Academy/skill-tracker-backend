package com.amalitech.user.service.repository;

import com.amalitech.user.service.model.UserSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
public interface UserSkillRepository extends JpaRepository<UserSkill, UUID> {
    /**
     * Deletes all UserSkill records associated with a specific user.
     * This is a "bulk delete" operation.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM UserSkill us WHERE us.user.id = :userId")
    void deleteByUserId(UUID userId);
}