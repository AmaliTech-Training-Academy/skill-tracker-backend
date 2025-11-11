package com.amalitech.user.service.dto.request;

import com.amalitech.user.service.model.enums.Role;
import com.amalitech.util.validation.ValidEnum;
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
        @ValidEnum(enumClass = Role.class, message = "Role must be either USER or ADMIN")
        Role role
) {}
