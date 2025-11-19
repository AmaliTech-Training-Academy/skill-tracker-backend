package com.amalitech.user.service.dto;

import com.amalitech.user.service.model.enums.GuidedTourStatus;
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
        GuidedTourStatus tourStatus,
        Boolean is_verified,
        PremiumTier premiumTier,
        GuidedTourStatus guidedTourStatus,
        String language,
        String timezone
) {}