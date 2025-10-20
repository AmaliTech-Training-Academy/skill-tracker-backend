package com.amalitech.user.service.mapper;

import com.amalitech.user.service.dto.response.UserDto;
import com.amalitech.user.service.model.User;
import org.springframework.stereotype.Component;

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