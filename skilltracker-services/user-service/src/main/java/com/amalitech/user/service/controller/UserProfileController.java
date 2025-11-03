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
 * This controller provides endpoints for authenticated users to retrieve, update,
 * and delete their own profiles, as well as an endpoint for retrieving any user's profile by ID (potentially for admin or public viewing).
 */
@RestController
@RequestMapping("/api/v1/users/profile")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    /**
     * Retrieves the profile of the currently authenticated user.
     * This endpoint allows a logged-in user to fetch their own detailed profile information.
     *
     * @param userDetails The authenticated user's details, typically obtained from the security context.
     * @return A ResponseEntity containing an ApiResponse with the authenticated user's profile.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = extractUserId(userDetails);
        UserProfileResponse profile = userProfileService.getUserProfile(userId);
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", profile, ""));
    }

    /**
     * Retrieves a user profile by their unique identifier.
     * This endpoint can be used to fetch the profile of any user, typically requiring
     * appropriate authorization (e.g., for administrative purposes or public profiles).
     *
     * @param userId The unique identifier (UUID) of the user whose profile is to be retrieved.
     * @return A ResponseEntity containing an ApiResponse with the requested user's profile.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(@PathVariable UUID userId) {
        UserProfileResponse profile = userProfileService.getUserProfile(userId);
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", profile, ""));
    }

    /**
     * Updates the profile of the currently authenticated user.
     * This endpoint allows a logged-in user to modify their own profile details.
     *
     * @param userDetails The authenticated user's details.
     * @param request     The request body containing the updated user profile information.
     * @return A ResponseEntity containing an ApiResponse with the updated user profile.
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
     * Partially updates the profile of the currently authenticated user.
     * This endpoint allows a logged-in user to modify specific fields of their own profile details.
     *
     * @param userDetails The authenticated user's details.
     * @param request     The request body containing the partial updated user profile information.
     * @return A ResponseEntity containing an ApiResponse with the updated user profile.
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
     * Deletes the profile of the currently authenticated user.
     * This endpoint allows a logged-in user to remove their own profile from the system.
     *
     * @param userDetails The authenticated user's details.
     * @return A ResponseEntity containing an ApiResponse indicating the successful deletion of the profile.
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteMyProfile(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = extractUserId(userDetails);
        userProfileService.deleteUserProfile(userId);
        return ResponseEntity.ok(ApiResponse.success("Profile deleted successfully", null, ""));
    }

    /**
     * Extracts the user ID from the provided UserDetails object.
     * This helper method safely casts UserDetails to CustomUserDetails to retrieve the UUID.
     *
     * @param userDetails The UserDetails object from which to extract the user ID.
     * @return The UUID of the user.
     * @throws IllegalStateException If the user ID cannot be extracted (e.g., UserDetails is not CustomUserDetails).
     */
    private UUID extractUserId(UserDetails userDetails) {
        if (userDetails instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getUser().getId();
        }
        throw new IllegalStateException("Unable to extract user ID from UserDetails");
    }
}
