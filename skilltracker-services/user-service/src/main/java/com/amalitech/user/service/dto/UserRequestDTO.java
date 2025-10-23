package com.amalitech.user.service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record UserRequestDTO(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password
) {}