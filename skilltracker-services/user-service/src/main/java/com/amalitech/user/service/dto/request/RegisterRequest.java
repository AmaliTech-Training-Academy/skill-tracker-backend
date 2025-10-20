package com.amalitech.user.service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        String email,
        @NotBlank @Size(min = 8) String password,
        @NotBlank String username
) {}
