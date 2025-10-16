package com.amalitech.user.service.model.enums;

/**
 * Defines the possible states of a user account throughout its lifecycle.
 */
public enum UserState {
    REGISTERED,
    VERIFIED,
    ONBOARDING_SKILLS,
    ONBOARDING_DIFFICULTY,
    ACTIVE,
    SUSPENDED,
    DELETED
}
