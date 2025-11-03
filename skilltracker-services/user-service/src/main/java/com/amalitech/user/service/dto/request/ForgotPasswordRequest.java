package com.amalitech.user.service.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(@Email(message = "Invalid Email Format") @NotBlank(message = "Email is Required") String email) {}
