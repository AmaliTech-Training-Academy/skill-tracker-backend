package com.amalitech.user.service.mapper;

import com.amalitech.user.service.dto.UserDto;
import com.amalitech.user.service.dto.UserSkillDto;
import com.amalitech.user.service.model.User;
import com.amalitech.user.service.model.UserSkill;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Manual mapper for converting User entity to UserDto, including nested UserSkill to UserSkillDto mapping.
 */
@Component
public class UserMapper {
    public static UserDto toDto(User user) {
        if (user == null) {
            return null;
        }

        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getState(),
                user.getPremiumTier(),
                user.getTourStatus()
        );

    }
}