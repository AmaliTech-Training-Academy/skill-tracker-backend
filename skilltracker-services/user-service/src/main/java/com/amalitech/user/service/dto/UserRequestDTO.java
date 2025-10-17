package com.amalitech.user.service.dto;

import com.amalitech.user.service.enums.roleEnum;
import com.amalitech.user.service.enums.stateEnum;
import com.amalitech.user.service.enums.tierEnum;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.util.Date;

@Builder
public record UserRequestDTO(
//        String userName,
//        @NotBlank(message = "Email is required")
//        @Email(message = "Invalid email")
//        @Size(max = 255)
//        @NotBlank(message = "Password is required")
//        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        String email,
        String password,
        Enum<roleEnum> role,
        Enum<stateEnum> state,
        Enum<tierEnum> PremiumTier,
        String language,
        String timezone, // Must validate datatype here
        Date last_login_at,
        Date updatedAt

) {}