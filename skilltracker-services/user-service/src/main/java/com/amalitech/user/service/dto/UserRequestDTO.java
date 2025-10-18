package com.amalitech.user.service.dto;

import com.amalitech.user.service.enums.roleEnum;
import com.amalitech.user.service.enums.stateEnum;
import com.amalitech.user.service.enums.tierEnum;
import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record UserRequestDTO(
        String email,
        String password,
        Enum<roleEnum> role,
        Enum<stateEnum> state,
        Enum<tierEnum> PremiumTier,
        String language,
        String timezone,
        LocalDateTime last_login_at,
        LocalDateTime updatedAt

) {}