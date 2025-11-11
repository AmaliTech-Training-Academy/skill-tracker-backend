package com.amalitech.user.service.dto.request;

import com.amalitech.user.service.model.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record CreateUserByAdminRequest(
        @NotBlank
        @Email
        String email,

        @NotNull(message = "Role is required")
        Role role
) {}
