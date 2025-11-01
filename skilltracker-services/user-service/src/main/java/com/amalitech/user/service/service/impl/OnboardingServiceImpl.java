package com.amalitech.user.service.service.impl;

import com.amalitech.common.event.events.UserOnboardingCompletedEvent;
import com.amalitech.user.service.dto.request.OnboardingRequest;
import com.amalitech.user.service.dto.request.SkillSelection;
import com.amalitech.user.service.events.EventProducer;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of the {@link OnboardingService} interface.
 * This service handles the business logic for completing a user's onboarding process,
 * including associating users with their selected skills and updating their account state.
 */
@Service
@RequiredArgsConstructor
public class OnboardingServiceImpl implements OnboardingService {

    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final UserSkillRepository userSkillRepository;
    private final EventProducer eventProducer;

    /**
     * {@inheritDoc}
     *
     * <p>This method performs the following actions:</p>
     * <ul>
     *     <li>Retrieves the user by their ID, throwing {@link EntityNotFoundException} if not found.</li>
     *     <li>Checks if the user has already completed onboarding (state is {@code ONBOARDED}),
     *         throwing {@link OnboardingAlreadyCompletedException} if so.</li>
     *     <li>Maps the provided skill selections to {@link UserSkill} entities.</li>
     *     <li>Persists all new {@link UserSkill} entities to the database.</li>
     *     <li>Updates the user's state to {@code ONBOARDED}.</li>
     *     <li>Publishes a {@link UserOnboardingCompletedEvent} to the event bus.</li>
     * </ul>
     *
     * @param userId  The unique identifier (UUID) of the user completing onboarding.
     * @param request The {@link OnboardingRequest} containing the user's selected skills and their proficiency levels.
     * @throws EntityNotFoundException          If the user with the given ID is not found.
     * @throws OnboardingAlreadyCompletedException If the user has already completed the onboarding process.
     */
    @Override
    @Transactional
    @PreAuthorize("hasAuthority('USER') and #userId == authentication.principal.user.id")
    public void completeOnboarding(UUID userId, OnboardingRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        if (user.getState() == UserState.ONBOARDED) {
            throw new OnboardingAlreadyCompletedException(user.getState().name());
        }

        List<UserSkill> newUserSkills = request.skills().stream()
                .map(skillDto -> createNewUserSkill(user, skillDto))
                .collect(Collectors.toList());

        userSkillRepository.saveAll(newUserSkills);

        user.setState(UserState.ONBOARDED);
        userRepository.save(user);

        List<UserOnboardingCompletedEvent.SkillSelectionData> selectedSkills =
            newUserSkills.stream()
                .map(userSkill -> UserOnboardingCompletedEvent.SkillSelectionData.builder()
                    .skillId(userSkill.getSkill().getId())
                    .skillName(userSkill.getSkill().getName())
                    .difficultyLevel(userSkill.getCurrentLevel().name())
                    .supportedTaskTypes(userSkill.getSkill().getSupportedTaskTypes())
                    .build())
                .collect(Collectors.toList());

        UserOnboardingCompletedEvent event = UserOnboardingCompletedEvent.builder()
                .userId(userId)
                .selectedSkills(selectedSkills)
                .build();

        eventProducer.publishOnboardingCompleted(event);
    }

    /**
     * Constructs a new {@link UserSkill} entity based on the user and selected skill data.
     * This private helper method encapsulates the logic for creating a {@code UserSkill} object,
     * including fetching the {@link Skill} entity and calculating initial experience points (XP).
     *
     * @param user     The {@link User} entity for whom the skill is being selected.
     * @param skillDto The {@link SkillSelection} DTO containing the skill ID and selected level.
     * @return A newly created {@link UserSkill} entity.
     * @throws EntityNotFoundException If the skill with the given ID in {@code skillDto} is not found.
     */
    private UserSkill createNewUserSkill(User user, SkillSelection skillDto) {
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