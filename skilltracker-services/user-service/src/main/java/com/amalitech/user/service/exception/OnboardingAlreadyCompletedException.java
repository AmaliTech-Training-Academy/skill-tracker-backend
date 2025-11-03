package com.amalitech.user.service.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when an onboarding operation is attempted on a user
 * who has already completed it (e.g., is in an 'ACTIVE' state).
 *
 * This will be translated by the GlobalExceptionHandler into a 409 CONFLICT response.
 */
@Getter
@ResponseStatus(HttpStatus.CONFLICT)
public class OnboardingAlreadyCompletedException extends RuntimeException {

    private final String state;

    public OnboardingAlreadyCompletedException(String state) {
        super(String.format("User has already completed onboarding. Current state is: %s", state));
        this.state = state;
    }
}