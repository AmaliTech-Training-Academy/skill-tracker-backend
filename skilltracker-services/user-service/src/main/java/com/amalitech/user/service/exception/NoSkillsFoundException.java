package com.amalitech.user.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when no skills are found in the system.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class NoSkillsFoundException extends RuntimeException {
    public NoSkillsFoundException(String message) {
        super(message);
    }
}
