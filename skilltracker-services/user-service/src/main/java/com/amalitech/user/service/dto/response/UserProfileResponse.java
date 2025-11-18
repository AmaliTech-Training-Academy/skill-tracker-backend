package com.amalitech.user.service.dto.response;

import com.amalitech.user.service.model.UserProfile;
import com.amalitech.user.service.model.enums.GuidedTourStatus;
import com.amalitech.user.service.model.enums.PremiumTier;
import com.amalitech.user.service.model.enums.Role;
import com.amalitech.user.service.model.enums.UserState;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for user profile information.
 * Combines User and UserProfile data for a complete profile view.
 */
public record UserProfileResponse(
        UUID id,
        String email,
        String fullName,
        String avatarUrl,
        String bio,
        UserState state,
        @JsonProperty("is_verified")
        Boolean isVerified,
        GuidedTourStatus tourStatus,
        Role role,
        PremiumTier premiumTier,
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
                profile.getUser().getId(),
                profile.getUser().getEmail(),
                profile.getFullName(),
                profile.getAvatarUrl(),
                profile.getBio(),
                profile.getUser().getState(),
                profile.getUser().getIsVerified(),
                profile.getUser().getTourStatus(),
                profile.getUser().getRole(),
                profile.getUser().getPremiumTier(),
                profile.getEmailNotifications(),
                profile.getPushNotifications(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
