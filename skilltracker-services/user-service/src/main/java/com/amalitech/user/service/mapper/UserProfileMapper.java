package com.amalitech.user.service.mapper;

import com.amalitech.user.service.dto.request.UpdateUserProfileRequest;
import com.amalitech.user.service.dto.response.UserProfileResponse;
import com.amalitech.user.service.model.UserProfile;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between UserProfile entity and DTOs.
 */
@Component
public class UserProfileMapper {

    /**
     * Converts a UserProfile entity to UserProfileResponse DTO.
     *
     * @param profile the user profile entity
     * @return the user profile response DTO
     */
    public UserProfileResponse toResponse(UserProfile profile) {
        if (profile == null) {
            return null;
        }

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

    /**
     * Creates a new UserProfile entity from default values.
     * Used during user registration.
     *
     * @return a new user profile with default settings
     */
    public UserProfile toEntity() {
        UserProfile profile = new UserProfile();
        profile.setEmailNotifications(true);
        profile.setPushNotifications(true);
        return profile;
    }

    /**
     * Updates an existing UserProfile entity with values from UpdateUserProfileRequest.
     * Only updates non-null fields (partial update).
     *
     * @param profile the existing user profile entity
     * @param request the update request
     */
    public void updateEntityFromRequest(UserProfile profile, UpdateUserProfileRequest request) {
        if (request == null) {
            return;
        }

        if (request.fullName() != null) {
            profile.setFullName(request.fullName());
        }
        if (request.avatarUrl() != null) {
            profile.setAvatarUrl(request.avatarUrl());
        }
        if (request.bio() != null) {
            profile.setBio(request.bio());
        }
        if (request.emailNotifications() != null) {
            profile.setEmailNotifications(request.emailNotifications());
        }
        if (request.pushNotifications() != null) {
            profile.setPushNotifications(request.pushNotifications());
        }
    }
}
