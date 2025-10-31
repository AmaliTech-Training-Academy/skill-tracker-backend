package com.amalitech.user.service.service.impl;

import com.amalitech.user.service.dto.request.OnboardingRequest;
import com.amalitech.user.service.dto.request.SkillSelectionDto;
import com.amalitech.user.service.exception.OnboardingAlreadyCompletedException;
import com.amalitech.user.service.model.Skill;
import com.amalitech.user.service.model.User;
import com.amalitech.user.service.model.UserSkill;
import com.amalitech.user.service.model.enums.UserState;
import com.amalitech.user.service.repository.SkillRepository;
import com.amalitech.user.service.repository.UserRepository;
import com.amalitech.user.service.repository.UserSkillRepository;
import com.amalitech.user.service.service.OnboardingService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OnboardingServiceImpl implements OnboardingService {

    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final UserSkillRepository userSkillRepository;

    @Override
    @Transactional
    public void completeOnboarding(UUID userId, OnboardingRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        if (user.getState() == UserState.ACTIVE) {
            throw new OnboardingAlreadyCompletedException(user.getState().name());
        }

        List<UserSkill> newUserSkills = request.skills().stream()
                .map(skillDto -> createNewUserSkill(user, skillDto))
                .collect(Collectors.toList());

        userSkillRepository.saveAll(newUserSkills);

        user.setState(UserState.ACTIVE);
        userRepository.save(user);
    }

    /**
     * Private helper method to construct a new UserSkill entity.
     * This logic is internal to the implementation.
     */
    private UserSkill createNewUserSkill(User user, SkillSelectionDto skillDto) {
        Skill skill = skillRepository.findById(skillDto.skillId())
                .orElseThrow(() -> new EntityNotFoundException("Skill not found with id" + skillDto.skillId()));

                        Long initialXp = skill.getLevelXpMap().getOrDefault(skillDto.level().name(), 0L);

        UserSkill userSkill = new UserSkill();
        userSkill.setUser(user);
        userSkill.setSkill(skill);
        userSkill.setCurrentLevel(skillDto.level());
        userSkill.setTotalXp(initialXp);

        return userSkill;
    }
}