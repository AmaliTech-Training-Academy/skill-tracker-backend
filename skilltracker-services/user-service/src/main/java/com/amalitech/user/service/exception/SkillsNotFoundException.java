package com.amalitech.user.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "Skills not found")
public class SkillsNotFoundException extends RuntimeException {

    public SkillsNotFoundException(UUID skillId) {
        super(String.format("Skill with id: '%s' not found", skillId));
    }

    public SkillsNotFoundException(Set<UUID> skillIds) {
        super(buildMessage(skillIds));
    }

    private static String buildMessage(Set<UUID> skillIds) {
        if (skillIds.size() == 1) {
            return String.format("Skill with id: '%s' not found", skillIds.iterator().next());
        }

        String ids = skillIds.stream()
                .map(UUID::toString)
                .collect(Collectors.joining("', '", "'", "'"));

        return String.format("Skills with ids: %s not found", ids);
    }
}
