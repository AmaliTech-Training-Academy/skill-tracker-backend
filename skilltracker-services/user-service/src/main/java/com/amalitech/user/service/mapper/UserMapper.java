package com.amalitech.user.service.mapper;

import com.amalitech.user.service.dto.UserRequestDTO;
import com.amalitech.user.service.dto.UserResponseDTO;
import com.amalitech.user.service.dto.response.OnboardingResponseDTO;
import com.amalitech.user.service.model.User;
import com.amalitech.user.service.model.enums.PremiumTier;
import com.amalitech.user.service.model.enums.Role;
import com.amalitech.user.service.model.enums.UserState;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Manual mapper for converting User entity to UserDto, including nested UserSkill to UserSkillDto mapping.
 */
@Component
public class UserMapper {

    private static final BCryptPasswordEncoder apiPasswordEncoder = new BCryptPasswordEncoder(12);

    public static User toEntity(UserRequestDTO dto) {
        if (dto == null) return null;
        User user = new User();
        user.setEmail(dto.email());
        user.setPasswordHash(apiPasswordEncoder.encode(dto.password()));
        user.setRole(Role.USER);
        user.setState(UserState.REGISTERED);
        user.setIsVerified(false);
        user.setPremiumTier(PremiumTier.FREE);
        user.setLanguage("en");
        user.setTimezone("UTC");
        user.setLastLoginAt(null);
        user.setUpdatedAt(LocalDateTime.now());

        return user;
    }

    public static UserResponseDTO toDto(User entity) {
        if (entity == null) return null;

        return new UserResponseDTO(
                entity.getId(),
                entity.getEmail(),
                entity.getUsername(),
                entity.getRole(),
                entity.getState(),
                entity.getTourStatus(),
                entity.getIsVerified(),
                entity.getPremiumTier(),
                entity.getLanguage(),
                entity.getTimezone()
        );
    }

    /**
     * Maps a User entity to the OnboardingResponseDTO.
     *
     * @param user The persisted User entity.
     * @return An OnboardingResponseDTO.
     */
    public OnboardingResponseDTO toOnboardingResponseDTO(User user) {
        if (user == null) {
            return null;
        }

        return new OnboardingResponseDTO(
                user.getId(),
                user.getEmail(),
                user.getState(),
                user.getTaskGenerationStatus(),
                user.getTourStatus(),
                user.getPremiumTier()
        );
    }
}