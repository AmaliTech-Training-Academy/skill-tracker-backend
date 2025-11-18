package com.amalitech.user.service.dto.response;

import com.amalitech.user.service.model.UserProfile;
import com.amalitech.user.service.model.enums.GuidedTourStatus;
import com.amalitech.user.service.model.enums.UserState;

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
        UserState userState,
        Boolean isVerified,
        GuidedTourStatus tourStatus,
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
                profile.getUser().getState(),
                profile.getUser().getIsVerified(),
                profile.getUser().getTourStatus(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
