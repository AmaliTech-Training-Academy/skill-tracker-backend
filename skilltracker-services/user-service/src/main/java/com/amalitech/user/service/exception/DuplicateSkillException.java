package com.amalitech.user.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when an attempt is made to create a resource that already exists
 * (e.g., a Skill with a duplicate name).
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateSkillException extends RuntimeException {

    public DuplicateSkillException(String message) {
        super(message);
    }
}