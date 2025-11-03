package com.amalitech.user.service.controller;

import com.amalitech.common.security.dto.response.ApiResponse;
import com.amalitech.user.service.dto.response.SkillResponse;
import com.amalitech.user.service.service.SkillService;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for managing skill-related operations.
 * This controller provides endpoints for retrieving available skills on the platform.
 */
@RestController
@RequestMapping("/api/v1/skills")
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    /**
     * Retrieves a paginated list of all available skills.
     *
     * @param pageable Pagination information (page number, size, sort).
     * @return A ResponseEntity containing an ApiResponse with a list of SkillResponse objects.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<SkillResponse>>> getAvailableSkills(
            Pageable pageable
    ) {
        List<SkillResponse> skills = skillService.getAllSkills(pageable);
        return ResponseEntity.ok(ApiResponse.success("Skill retrieved successfully", skills, null));
    }
}