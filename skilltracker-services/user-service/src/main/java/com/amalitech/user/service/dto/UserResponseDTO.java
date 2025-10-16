package com.amalitech.user.service.dto;

import com.amalitech.user.service.enums.roleEnum;
import com.amalitech.user.service.enums.stateEnum;
import com.amalitech.user.service.enums.tierEnum;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.Date;

@Builder
public record UserResponseDTO(
//        @NotBlank(message = "Full name is required")
//        @Size(max = 100)
//        @NotBlank(message = "Email is required")
//        @Email(message = "Invalid email")
//        @Size(max = 255)
        String userName,
        Long id,
        String email,
        Enum<roleEnum> role,
        Enum<stateEnum> state,
        Enum<tierEnum> PremiumTier,
        String language,
        String timezone,
        Date last_login_at,
        Date updatedAt
        ) {}