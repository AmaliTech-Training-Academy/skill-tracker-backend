package com.amalitech.user.service.controller;

import com.amalitech.common.security.dto.response.ApiResponse;
import com.amalitech.user.service.dto.request.TourStatusRequest;
import com.amalitech.user.service.dto.response.UserStatusDTO;
import com.amalitech.user.service.security.CustomUserDetails;
import com.amalitech.user.service.service.UserTourService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserTourController {

    private final UserTourService userTourService;

    public UserTourController(UserTourService userTourService) {
        this.userTourService = userTourService;
    }

    /**
     * Updates the current user's guided tour status.
     * @param request The new guided tour status
     * @return Updated User DTO
     */
    @PatchMapping("/tour-status")
    public ResponseEntity<ApiResponse<UserStatusDTO>> updateTourStatus(
            @RequestBody TourStatusRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        UUID userId = extractUserId(userDetails);

        UserStatusDTO updatedUser = userTourService.updateTourStatus(userId, request.guidedTourStatus());
        return ResponseEntity.ok(ApiResponse.success("Tour status updated successfully", updatedUser, null));
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
