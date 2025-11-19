package com.amalitech.user.service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(@NotBlank(message = "Old password is required")String oldPassword,
                                    @NotBlank(message = "New password is required")
                                    @Size(min = 8, message = "Password must be at least 8 characters long")
                                    @Pattern(
                                            regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}",
                                            message = "Password must contain at least 1 uppercase letter, 1 lowercase letter, 1 number, and 1 special character"
                                    ) String newPassword) {
}
