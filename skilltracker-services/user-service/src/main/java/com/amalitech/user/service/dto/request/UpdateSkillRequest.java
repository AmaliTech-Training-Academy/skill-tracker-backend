package com.amalitech.user.service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * DTO for updating existing skills through the admin API.
 * Contains only the fields that can be updated.
 */
public record UpdateSkillRequest(

    @NotBlank(message = "Skill name is required")
    String name,

    @NotBlank(message = "Skill description is required")
    String description,

    @NotBlank(message = "Skill category is required")
    String category,

    String iconUrl,

    @NotEmpty(message = "Supported task types cannot be empty")
    List<@NotBlank(message = "Task type cannot be blank") String> supportedTaskTypes

) {}
