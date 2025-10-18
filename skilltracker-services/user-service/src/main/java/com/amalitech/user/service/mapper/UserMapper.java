package com.amalitech.user.service.mapper;

import com.amalitech.user.service.dto.UserRequestDTO;
import com.amalitech.user.service.dto.UserResponseDTO;
import com.amalitech.user.service.model.User;
import org.springframework.stereotype.Component;

/**
 * Manual mapper for converting User entity to UserDto, including nested UserSkill to UserSkillDto mapping.
 */
@Component
public class UserMapper {

    public static User toEntity(UserRequestDTO dto) {
        if (dto == null) return null;
        return User.builder()
                .email(dto.email())
                .passwordHash(dto.password())
                .build();
    }

    public static UserResponseDTO toDto(User entity) {
        if (entity == null) return null;
        return new UserResponseDTO(
                entity.getId(),
                entity.getEmail(),
                entity.getUsername(),
                entity.getRole(),
                entity.getState(),
                entity.getPremiumTier(),
                entity.getLanguage(),
                entity.getTimezone(),
                entity.getUpdatedAt(),
                entity.getIsVerified());
    }
}