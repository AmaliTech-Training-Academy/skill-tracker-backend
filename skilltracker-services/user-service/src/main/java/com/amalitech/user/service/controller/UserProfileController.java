package com.amalitech.user.service.controller;

import com.amalitech.common.security.dto.response.ApiResponse;
import com.amalitech.user.service.dto.request.UpdateUserProfileRequest;
import com.amalitech.user.service.dto.response.UserProfileResponse;
import com.amalitech.user.service.security.CustomUserDetails;
import com.amalitech.user.service.service.UserProfileService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for managing user profile operations.
 * Provides endpoints for retrieving and updating user profiles.
 */
@RestController
@RequestMapping("/api/v1/users/profile")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    /**
     * Retrieves the authenticated user's profile.
     *
     * @param userDetails the authenticated user details
     * @return the user profile response wrapped in ApiResponse
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = extractUserId(userDetails);
        UserProfileResponse profile = userProfileService.getUserProfile(userId);
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", profile, ""));
    }

    /**
     * Retrieves a user profile by user ID (admin or public endpoint).
     *
     * @param userId the user ID
     * @return the user profile response wrapped in ApiResponse
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(@PathVariable UUID userId) {
        UserProfileResponse profile = userProfileService.getUserProfile(userId);
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", profile, ""));
    }

    /**
     * Updates the authenticated user's profile.
     *
     * @param userDetails the authenticated user details
     * @param request the update request
     * @return the updated user profile response wrapped in ApiResponse
     */
    @PutMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        UUID userId = extractUserId(userDetails);
        UserProfileResponse updatedProfile = userProfileService.updateUserProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success("User profile updated successfully", updatedProfile, ""));
    }

    /**
     * Partially updates the authenticated user's profile.
     *
     * @param userDetails the authenticated user details
     * @param request the update request
     * @return the updated user profile response wrapped in ApiResponse
     */
    @PatchMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> patchMyProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        UUID userId = extractUserId(userDetails);
        UserProfileResponse updatedProfile = userProfileService.updateUserProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updatedProfile, ""));
    }

    /**
     * Deletes the authenticated user's profile.
     *
     * @param userDetails the authenticated user details
     * @return no content response wrapped in ApiResponse
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteMyProfile(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = extractUserId(userDetails);
        userProfileService.deleteUserProfile(userId);
        return ResponseEntity.ok(ApiResponse.success("Profile deleted successfully", null, ""));
    }

    /**
     * Extracts the user ID from UserDetails.
     */
    private UUID extractUserId(UserDetails userDetails) {
        if (userDetails instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getUser().getId();
        }
        throw new IllegalStateException("Unable to extract user ID from UserDetails");
    }
}
