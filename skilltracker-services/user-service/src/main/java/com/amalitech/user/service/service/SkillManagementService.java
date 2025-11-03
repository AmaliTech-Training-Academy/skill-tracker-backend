package com.amalitech.user.service.service;

import com.amalitech.user.service.dto.request.CreateSkillRequest;
import com.amalitech.user.service.dto.request.UpdateSkillRequest;
import com.amalitech.user.service.dto.response.SkillResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for managing skill operations.
 * Provides CRUD operations for skills with proper validation and event publishing.
 */
public interface SkillManagementService {

    /**
     * Creates a new skill from the provided request.
     *
     * @param request The skill creation request containing all required data
     * @return The created skill response
     */
    SkillResponse createSkill(CreateSkillRequest request);

    /**
     * Updates an existing skill with the provided data.
     *
     * @param skillId The ID of the skill to update
     * @param request The skill entity with updated data
     * @return The updated skill entity
     */
    SkillResponse updateSkill(UUID skillId, UpdateSkillRequest request);

    /**
     * Deletes a skill by its ID.
     *
     * @param skillId The ID of the skill to delete
     */
    void deleteSkill(UUID skillId);

    /**
     * Retrieves a skill by its ID.
     *
     * @param skillId The ID of the skill to retrieve
     * @return The skill entity
     */
    SkillResponse getSkill(UUID skillId);

    Page<SkillResponse> getAllSkills(Pageable pageable);
}
