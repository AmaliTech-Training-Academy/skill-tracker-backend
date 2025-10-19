package com.amalitech.user.service.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(@NotBlank String token, @NotBlank String password) {
}
