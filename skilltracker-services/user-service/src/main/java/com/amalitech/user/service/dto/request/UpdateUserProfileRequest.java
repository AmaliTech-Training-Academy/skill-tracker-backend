package com.amalitech.user.service.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating user profile information.
 * All fields are optional to support partial updates.
 */
public record UpdateUserProfileRequest(

        @Size(max = 255, message = "Full name must not exceed 255 characters")
        String fullName,

        String avatarUrl,

        @Size(max = 1000, message = "Bio must not exceed 1000 characters")
        String bio,

        Boolean emailNotifications,

        Boolean pushNotifications
) {
}