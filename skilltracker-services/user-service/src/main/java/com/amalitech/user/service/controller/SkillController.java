package com.amalitech.user.service.controller;

import com.amalitech.common.security.dto.response.ApiResponse;
import com.amalitech.user.service.dto.response.SkillResponseDto;
import com.amalitech.user.service.service.SkillService;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/skills")
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SkillResponseDto>>> getAvailableSkills(
            Pageable pageable
    ) {
        List<SkillResponseDto> skills = skillService.getAllSkills(pageable);
        return ResponseEntity.ok(ApiResponse.success("Skill retrieved successfully", skills, null));
    }
}