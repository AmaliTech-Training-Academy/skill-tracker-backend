package com.amalitech.user.service.model;

import lombok.Getter;


import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class VerificationObject {
    private final UUID userId;
    private final int verificationCode;
    private final LocalDateTime expirationTime = LocalDateTime.now().plusMinutes(10);
    LocalDateTime createdAt = LocalDateTime.now();
    LocalDateTime validatedAt;

    public VerificationObject(UUID userId, int verificationCode) {
        this.userId = userId;
        this.verificationCode = verificationCode;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expirationTime);
    }

    public void markAsValidated() {
        this.validatedAt = LocalDateTime.now();
    }
}
