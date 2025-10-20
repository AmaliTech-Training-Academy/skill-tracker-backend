package com.amalitech.user.service.service.impl;

import com.amalitech.user.service.dto.request.UpdateUserProfileRequest;
import com.amalitech.user.service.dto.response.UserProfileResponse;
import com.amalitech.user.service.exception.ProfileNotFoundException;
import com.amalitech.user.service.mapper.UserProfileMapper;
import com.amalitech.user.service.model.UserProfile;
import com.amalitech.user.service.repository.UserProfileRepository;
import com.amalitech.user.service.service.UserProfileService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementation of UserProfileService for managing user profile operations.
 */
@Service
public class UserProfileServiceImpl implements UserProfileService {

    private static final Logger log = LoggerFactory.getLogger(UserProfileServiceImpl.class);

    private final UserProfileRepository userProfileRepository;
    private final UserProfileMapper userProfileMapper;

    public UserProfileServiceImpl(
            UserProfileRepository userProfileRepository,
            UserProfileMapper userProfileMapper) {
        this.userProfileRepository = userProfileRepository;
        this.userProfileMapper = userProfileMapper;
    }

    /**
     * Retrieves a user profile by user ID.
     *
     * @param userId the UUID of the user
     * @return the user profile response
     * @throws ProfileNotFoundException if the profile is not found
     */
    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(UUID userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ProfileNotFoundException("Profile not found for user: " + userId));

        log.debug("Retrieved profile for user: {}", userId);
        return userProfileMapper.toResponse(profile);
    }

    /**
     * Updates a user profile with the provided information.
     * Only non-null fields in the request will be updated.
     *
     * @param userId the UUID of the user
     * @param request the update request containing new profile data
     * @return the updated user profile response
     * @throws ProfileNotFoundException if the profile is not found
     */
    @Override
    @Transactional
    public UserProfileResponse updateUserProfile(UUID userId, UpdateUserProfileRequest request) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ProfileNotFoundException("Profile not found for user: " + userId));

        userProfileMapper.updateEntityFromRequest(profile, request);

        UserProfile updatedProfile = userProfileRepository.save(profile);
        log.info("Updated profile for user: {}", userId);

        return userProfileMapper.toResponse(updatedProfile);
    }

    /**
     * Deletes a user profile.
     *
     * @param userId the UUID of the user
     * @throws ProfileNotFoundException if the profile is not found
     */
    @Override
    @Transactional
    public void deleteUserProfile(UUID userId) {
        if (!userProfileRepository.existsByUserId(userId)) {
            throw new ProfileNotFoundException("Profile not found for user: " + userId);
        }

        userProfileRepository.deleteByUserId(userId);
        log.info("Deleted profile for user: {}", userId);
    }
}
