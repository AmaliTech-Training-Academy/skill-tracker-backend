package com.amalitech.user.service.mapper;

import com.amalitech.user.service.dto.UserRequestDTO;
import com.amalitech.user.service.dto.UserResponseDTO;
import com.amalitech.user.service.enums.tierEnum;
import com.amalitech.user.service.model.User;
import com.amalitech.user.service.model.enums.PremiumTier;
import com.amalitech.user.service.model.enums.Role;
import com.amalitech.user.service.model.enums.UserState;
import com.amalitech.user.service.util.PasswordEncoderUtil;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

/**
 * Manual mapper for converting User entity to UserDto, including nested UserSkill to UserSkillDto mapping.
 */


@Component
public class UserMapper {

    public static User toEntity(UserRequestDTO dto) {
        if (dto == null) return null;
        return User.builder()
                .email(dto.email())
                .username(dto.username())
                .passwordHash(PasswordEncoderUtil.encodePassword(dto.password()))
                .role(Role.USER)
                .state(UserState.REGISTERED)
                .isVerified(false)
                .premiumTier(PremiumTier.FREE)
                .language("en")
                .timezone("UTC")
                .lastLoginAt(null)
                .updatedAt(LocalDateTime.now())
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