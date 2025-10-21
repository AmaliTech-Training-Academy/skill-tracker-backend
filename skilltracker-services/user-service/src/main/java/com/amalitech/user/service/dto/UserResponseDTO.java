package com.amalitech.user.service.dto;

import com.amalitech.user.service.model.enums.PremiumTier;
import com.amalitech.user.service.model.enums.Role;
import com.amalitech.user.service.model.enums.UserState;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record UserResponseDTO(
        UUID id,
        String email,
        String username,
        Role role,
        UserState state,
        Boolean isVerified,
        PremiumTier premiumTier,
        String language,
        String timezone,
        LocalDateTime updatedAt,
        LocalDateTime lastLoginAt
) {}