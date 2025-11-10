package com.amalitech.task.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;


@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "Skills not found")
public class SkillsNotFoundException extends RuntimeException {
    public SkillsNotFoundException(UUID skillId) {
        super(String.format("Skill with id: '%s' not found", skillId));
    }
}
