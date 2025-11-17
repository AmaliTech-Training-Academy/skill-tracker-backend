package com.amalitech.user.service.dto.request;

import com.amalitech.user.service.model.enums.Role;
import com.amalitech.util.validation.ValidEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

/**
 * Request object for creating a new user by an administrator.
 *
 * @param email the email address of the new user (must be unique)
 * @param role the role to assign to the new user
 */
@Builder
public record CreateUserByAdminRequest(
        @NotBlank
        @Email
        String email,

        @NotNull(message = "Role is required")
        @ValidEnum(enumClass = Role.class, message = "Role must be either USER or ADMIN")
        Role role
) {}
