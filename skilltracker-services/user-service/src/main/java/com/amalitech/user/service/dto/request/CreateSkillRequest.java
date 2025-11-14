package com.amalitech.user.service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

/**
 * DTO for creating new skills through the admin API.
 * Contains only the fields needed for skill creation.
 * Note: name and category are automatically converted to uppercase.
 */
public record CreateSkillRequest(

        @NotBlank(message = "Skill name is required")
        String name,

        @NotBlank(message = "Skill description is required")
        String description,

        @NotBlank(message = "Skill category is required")
        String category,

        String iconUrl,

        @NotEmpty(message = "Supported task types cannot be empty")
        Set<@NotBlank(message = "Task type cannot be blank") String> supportedTaskTypes

) {
    /**
     * Compact constructor that automatically converts name and category to uppercase.
     */
    public CreateSkillRequest {
        name = name != null ? name.toUpperCase().trim() : null;
        category = category != null ? category.toUpperCase().trim() : null;
    }
}
