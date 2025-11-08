package com.amalitech.task.service.repository;

import com.amalitech.task.service.model.UserSkillProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSkillProfileRepository extends JpaRepository<UserSkillProfile, UserSkillProfile.UserSkillId> {

    /**
     * Finds a user's skill profile by their ID (which is nested in the 'id' field)
     * and the name of the skill.
     *
     * Spring Data JPA understands this naming convention:
     * "findBy"
     * "Id" (the name of the @EmbeddedId field)
     * "UserId" (the name of the field *inside* UserSkillId)
     * "And"
     * "SkillName" (the top-level field in UserSkillProfile)
     */
    Optional<UserSkillProfile> findByIdUserIdAndSkillName(UUID userId, String skillName);
}