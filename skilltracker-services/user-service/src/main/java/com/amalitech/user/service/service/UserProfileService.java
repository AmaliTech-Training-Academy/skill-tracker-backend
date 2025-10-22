package com.amalitech.user.service.service;

import com.amalitech.user.service.dto.request.UpdateUserProfileRequest;
import com.amalitech.user.service.dto.response.UserProfileResponse;

import java.util.UUID;

/**
 * Service interface for managing user profile operations.
 */
public interface UserProfileService {
    /**
     * Retrieves a user profile by user ID.
     *
     * @param userId the UUID of the user
     * @return the user profile response
     */
    UserProfileResponse getUserProfile(UUID userId);

    /**
     * Updates a user profile.
     *
     * @param userId the UUID of the user
     * @param request the update request containing new profile data
     * @return the updated user profile response
     */
    UserProfileResponse updateUserProfile(UUID userId, UpdateUserProfileRequest request);

    /**
     * Deletes a user profile.
     *
     * @param userId the UUID of the user
     */
    void deleteUserProfile(UUID userId);
}
