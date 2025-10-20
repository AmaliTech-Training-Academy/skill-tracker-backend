package com.amalitech.user.service.dto.response;

import com.amalitech.user.service.model.UserProfile;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for user profile information.
 */
public record UserProfileResponse(
        UUID userId,
        String fullName,
        String avatarUrl,
        String bio,
        Boolean emailNotifications,
        Boolean pushNotifications,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /**
     * Creates a UserProfileResponse from a UserProfile entity.
     *
     * @param profile the user profile entity
     * @return the response DTO
     */
    public static UserProfileResponse from(UserProfile profile) {
        return new UserProfileResponse(
                profile.getUserId(),
                profile.getFullName(),
                profile.getAvatarUrl(),
                profile.getBio(),
                profile.getEmailNotifications(),
                profile.getPushNotifications(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
