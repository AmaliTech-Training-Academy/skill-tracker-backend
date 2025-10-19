package com.amalitech.user.service.dto;

import lombok.Builder;

@Builder
public record UserRequestDTO(
        String email,
        String username,
        String password
) {}