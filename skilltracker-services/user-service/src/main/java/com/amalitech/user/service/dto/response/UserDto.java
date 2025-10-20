package com.amalitech.user.service.dto.response;


import com.amalitech.user.service.model.enums.PremiumTier;
import com.amalitech.user.service.model.enums.TourStatus;
import com.amalitech.user.service.model.enums.UserState;

import java.util.UUID;

public record UserDto(
        UUID id,
        String email,
        String username,
        UserState state,
        PremiumTier premiumTier,
        TourStatus tourStatus
) {}
