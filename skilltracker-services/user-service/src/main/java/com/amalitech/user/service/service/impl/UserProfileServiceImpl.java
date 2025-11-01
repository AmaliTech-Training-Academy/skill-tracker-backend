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
 * Implementation of the {@link UserProfileService} interface.
 * This service provides concrete implementations for managing user profile operations,
 * including retrieving, updating, and deleting user profiles.
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
     * {@inheritDoc}
     * <p>Retrieves a user profile by their unique user ID.</p>
     *
     * @param userId The UUID of the user whose profile is to be retrieved.
     * @return The {@link UserProfileResponse} containing the user's profile data.
     * @throws ProfileNotFoundException If no user profile is found for the given {@code userId}.
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
     * {@inheritDoc}
     * <p>Updates an existing user profile with the provided information.
     * Only non-null fields in the {@code request} will be updated in the profile.</p>
     *
     * @param userId  The UUID of the user whose profile is to be updated.
     * @param request The {@link UpdateUserProfileRequest} containing the new profile data.
     * @return The {@link UserProfileResponse} containing the updated user's profile data.
     * @throws ProfileNotFoundException If no user profile is found for the given {@code userId}.
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
     * {@inheritDoc}
     * <p>Deletes a user profile associated with the given user ID.</p>
     *
     * @param userId The UUID of the user whose profile is to be deleted.
     * @throws ProfileNotFoundException If no user profile is found for the given {@code userId}.
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
