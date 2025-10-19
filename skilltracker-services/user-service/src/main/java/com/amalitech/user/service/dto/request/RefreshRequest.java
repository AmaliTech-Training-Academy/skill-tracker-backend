package com.amalitech.user.service.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(@NotBlank(message = "Refresh token is required") String token) {
}
