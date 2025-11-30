package com.amalitech.task.service.repository;

import com.amalitech.task.service.model.UserSkillProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface UserSkillProfileRepository extends JpaRepository<UserSkillProfile, UserSkillProfile.UserSkillId> {

    /**
     * Finds a user's skill profile by their ID (which is nested in the 'id' field)
     * and the name of the skill (case-insensitive).
     */
    @Query("SELECT usp FROM UserSkillProfile usp WHERE usp.id.userId = :userId AND LOWER(usp.skillName) = LOWER(:skillName)")
    Optional<UserSkillProfile> findByIdUserIdAndSkillName(@Param("userId") UUID userId, @Param("skillName") String skillName);

    /**
     * Finds all skill IDs associated with a user.
     * Used to fetch tasks for all skills the user has in their profile.
     *
     * @param userId The ID of the user
     * @return A set of skill IDs
     */
    @Query("SELECT usp.id.skillId FROM UserSkillProfile usp WHERE usp.id.userId = :userId")
    Set<UUID> findSkillIdsByUserId(@Param("userId") UUID userId);

    /**
     * Finds all skill profiles for a given user.
     * We need this to get the (skillId, difficulty) pairs.
     * The method name is based on the embedded ID's field name 'userId'.
     */
    List<UserSkillProfile> findById_UserId(UUID userId);
}