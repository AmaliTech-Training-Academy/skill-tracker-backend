package com.amalitech.user.service.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class VerificationObject {
    private final UUID userId;
    private final int verificationCode;
    private final int expirationTime;
    LocalDateTime createdAt;
    LocalDateTime validatedAt;

    public VerificationObject(UUID userId, int verificationCode, int expirationTime) {
        this.userId = userId;
        this.verificationCode = verificationCode;
        this.createdAt = LocalDateTime.now();
        this.expirationTime = expirationTime;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(createdAt.plusMinutes(expirationTime));
    }

    public boolean canBeValidated() {
        return validatedAt == null && !isExpired();
    }

    public void markAsValidated() {
        this.validatedAt = LocalDateTime.now();
    }
}
