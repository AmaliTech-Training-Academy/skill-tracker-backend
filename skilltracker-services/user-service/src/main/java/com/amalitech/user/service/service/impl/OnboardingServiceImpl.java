package com.amalitech.user.service.service.impl;

import com.amalitech.common.event.events.UserOnboardingCompletedEvent;
import com.amalitech.user.service.dto.request.OnboardingRequest;
import com.amalitech.user.service.dto.request.SkillSelection;
import com.amalitech.user.service.dto.response.OnboardingResponseDTO;
import com.amalitech.user.service.events.EventProducer;
import com.amalitech.user.service.exception.OnboardingAlreadyCompletedException;
import com.amalitech.user.service.mapper.UserMapper;
import com.amalitech.user.service.model.Skill;
import com.amalitech.user.service.model.User;
import com.amalitech.user.service.model.UserSkill;
import com.amalitech.user.service.model.enums.TaskGenerationStatus;
import com.amalitech.user.service.model.enums.UserState;
import com.amalitech.user.service.repository.SkillRepository;
import com.amalitech.user.service.repository.UserRepository;
import com.amalitech.user.service.repository.UserSkillRepository;
import com.amalitech.user.service.service.OnboardingService;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementation of the {@link OnboardingService} interface.
 * This service handles the business logic for completing a user's onboarding process.
 *
 * Revisions focus on:
 * 1. Performance: Eliminating N+1 queries by pre-fetching all selected skills.
 * 2. Resilience: Decoupling event publishing from the DB transaction using
 * TransactionSynchronizationManager to publish *after* successful commit.
 * 3. Validation: Validating all skill IDs upfront.
 */
@Service
@Slf4j
public class OnboardingServiceImpl implements OnboardingService {

    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final UserSkillRepository userSkillRepository;
    private final EventProducer eventProducer;
    private final UserMapper userMapper;

    public OnboardingServiceImpl(UserRepository userRepository,
                                 SkillRepository skillRepository,
                                 UserSkillRepository userSkillRepository,
                                 EventProducer eventProducer,
                                 UserMapper userMapper) {
        this.userRepository = userRepository;
        this.skillRepository = skillRepository;
        this.userSkillRepository = userSkillRepository;
        this.eventProducer = eventProducer;
        this.userMapper = userMapper;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This method orchestrates the onboarding process by delegating to
     * private methods for validation, persistence, and event registration.
     * The entire operation is atomic due to @Transactional.
     * </p>
     */
    @Override
    @Transactional
    @PreAuthorize("hasAuthority('USER') and #userId == authentication.principal.user.id")
    public OnboardingResponseDTO completeOnboarding(UUID userId, OnboardingRequest request) {

        User user = findAndValidateUser(userId);
        Map<UUID, Skill> foundSkillsMap = validateAndFetchSkills(request);

        List<UserSkill> newUserSkills = persistUserSkills(user, request, foundSkillsMap);

        user.setState(UserState.ONBOARDED);
        user.setTaskGenerationStatus(TaskGenerationStatus.PENDING);
        User savedUser = userRepository.save(user);

        buildAndRegisterOnboardingEvent(savedUser.getId(), newUserSkills);

        return userMapper.toOnboardingResponseDTO(savedUser);
    }

    /**
     * Finds the user and validates they are in a state to be onboarded.
     */
    private User findAndValidateUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        if (user.getState() == UserState.ONBOARDED) {
            throw new OnboardingAlreadyCompletedException(user.getState().name());
        }

        return user;
    }

    /**
     * Fetches all requested skills in a single batch and validates that all exist.
     */
    private Map<UUID, Skill> validateAndFetchSkills(OnboardingRequest request) {
        Set<UUID> requestedSkillIds = request.skills().stream()
                .map(SkillSelection::skillId)
                .collect(Collectors.toSet());

        Map<UUID, Skill> foundSkillsMap = skillRepository.findAllById(requestedSkillIds).stream()
                .collect(Collectors.toMap(Skill::getId, Function.identity()));

        if (foundSkillsMap.size() != requestedSkillIds.size()) {
            Set<UUID> missingIds = requestedSkillIds.stream()
                    .filter(id -> !foundSkillsMap.containsKey(id))
                    .collect(Collectors.toSet());
            throw new EntityNotFoundException("Could not find skills with IDs: " + missingIds);
        }
        return foundSkillsMap;
    }

    /**
     * Creates and persists all UserSkill entities and updates the User's state.
     * This method is now idempotent: it deletes any existing UserSkill
     * records for the user before inserting the new ones. This cleans up
     * any orphaned data from a previously failed onboarding (Saga rollback).
     */
    private List<UserSkill> persistUserSkills(User user, OnboardingRequest request, Map<UUID, Skill> foundSkillsMap) {

        log.info("Deleting existing UserSkill records for user: {}", user.getId());
        userSkillRepository.deleteByUserId(user.getId());

        List<UserSkill> newUserSkills = request.skills().stream()
                .map(skillDto -> {
                    Skill skill = foundSkillsMap.get(skillDto.skillId());
                    return createNewUserSkill(user, skill, skillDto);
                })
                .collect(Collectors.toList());

        return userSkillRepository.saveAll(newUserSkills);
    }

    /**
     * Builds the onboarding event and registers it to publish *after*
     * the database transaction successfully commits.
     */
    private void buildAndRegisterOnboardingEvent(UUID userId, List<UserSkill> newUserSkills) {
        List<UserOnboardingCompletedEvent.SkillSelectionData> selectedSkills =
                newUserSkills.stream()
                        .map(userSkill -> UserOnboardingCompletedEvent.SkillSelectionData.builder()
                                .skillId(userSkill.getSkill().getId())
                                .skillName(userSkill.getSkill().getName())
                                .difficultyLevel(userSkill.getCurrentLevel().name())
                                .supportedTaskTypes(new HashSet<>(userSkill.getSkill().getSupportedTaskTypes()))
                                .build())
                        .collect(Collectors.toList());

        UserOnboardingCompletedEvent event = UserOnboardingCompletedEvent.builder()
                .userId(userId)
                .selectedSkills(selectedSkills)
                .build();

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            eventProducer.publishOnboardingCompleted(event);
                            log.info(
                                    "Onboarding event for user {} queued for publishing (with retries).",
                                    userId
                            );
                        } catch (Exception e) {
                            log.error(
                                    "CRITICAL: Onboarding DB commit succeeded but event publish failed for user {}. Downstream systems will be out of sync.",
                                    userId,
                                    e
                            );
                        }
                    }
                }
        );
    }

    /**
     * Constructs a new {@link UserSkill} entity.
     *
     * @param user     The {@link User} entity.
     * @param skill    The pre-fetched {@link Skill} entity.
     * @param skillDto The {@link SkillSelection} DTO.
     * @return A newly created {@link UserSkill} entity.
     */
    private UserSkill createNewUserSkill(User user, Skill skill, SkillSelection skillDto) {
        Long initialXp = skill.getLevelXpMap().getOrDefault(skillDto.level().name(), 0L);

        UserSkill userSkill = new UserSkill();
        userSkill.setUser(user);
        userSkill.setSkill(skill);
        userSkill.setCurrentLevel(skillDto.level());
        userSkill.setTotalXp(initialXp);

        return userSkill;
    }
}