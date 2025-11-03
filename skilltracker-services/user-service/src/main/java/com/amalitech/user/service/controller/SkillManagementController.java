package com.amalitech.user.service.controller;

import com.amalitech.common.security.dto.response.ApiResponse;
import com.amalitech.user.service.dto.request.CreateSkillRequest;
import com.amalitech.user.service.dto.request.UpdateSkillRequest;
import com.amalitech.user.service.dto.response.SkillResponse;
import com.amalitech.user.service.service.SkillManagementService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/skills")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAuthority('ADMIN')")
public class SkillManagementController {

    private final SkillManagementService skillManagementService;


    /**
     * Retrieves a paginated list of all skills.
     * To use: GET /api/v1/admin/skills?page=0&size=10&sort=name,asc
     *
     * @param pageable Pagination information (page number, size, sort).
     * @return A ResponseEntity containing an ApiResponse with a Page of SkillResponse objects.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<SkillResponse>>> getAllSkills(Pageable pageable) {
        log.info("Admin retrieving all skills, page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());

        Page<SkillResponse> skillResponsePage = skillManagementService.getAllSkills(pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Skills retrieved successfully", skillResponsePage, null));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SkillResponse>> createSkill(@Valid @RequestBody CreateSkillRequest request) {
        SkillResponse created = skillManagementService.createSkill(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Skill created successfully", created, ""));
    }

    @PutMapping("/{skillId}")
    public ResponseEntity<ApiResponse<SkillResponse>> updateSkill(
            @PathVariable UUID skillId,
            @Valid @RequestBody UpdateSkillRequest request) {
        SkillResponse updated = skillManagementService.updateSkill(skillId, request);
        return ResponseEntity.ok(
                ApiResponse.success("Skill updated successfully", updated, ""));
    }

    @DeleteMapping("/{skillId}")
    public ResponseEntity<ApiResponse<Void>> deleteSkill(@PathVariable UUID skillId) {
        skillManagementService.deleteSkill(skillId);
        return ResponseEntity.ok(
                ApiResponse.success("Skill deleted successfully", null, ""));
    }


    @GetMapping("/{skillId}")
    public ResponseEntity<ApiResponse<SkillResponse>> getSkill(@PathVariable UUID skillId) {
        SkillResponse skill = skillManagementService.getSkill(skillId);
        return ResponseEntity.ok(
                ApiResponse.success("Skill retrieved successfully", skill, ""));
    }
}