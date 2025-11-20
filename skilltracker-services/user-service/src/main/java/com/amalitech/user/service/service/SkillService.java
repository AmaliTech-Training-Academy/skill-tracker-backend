package com.amalitech.user.service.service;


import com.amalitech.user.service.dto.response.SkillResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for managing and retrieving Skill data.
 * Defines the contract for skill-related operations.
 */
public interface SkillService {

    /**
     * Retrieves a list of all available skills, formatted for frontend display.
     *
     * @return A list of SkillResponseDto objects.
     */
    List<SkillResponse> getAllSkills(Pageable pageable);
    /**
     * Retrieves all skills associated with a specific user.
     *
     * @param userId the ID of the user
     * @return a list of UserSkillDto
     */
    java.util.List<com.amalitech.user.service.dto.UserSkillDto> getUserSkills(java.util.UUID userId);
}