package com.amalitech.analytics.service.events;

import com.amalitech.analytics.service.model.UserSkillEvent;
import com.amalitech.analytics.service.model.UserSkillSelection;
import com.amalitech.analytics.service.model.enums.SkillEventSource;
import com.amalitech.analytics.service.repository.UserSkillEventRepository;
import com.amalitech.common.event.events.UserOnboardingCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSkillAnalyticsListener {

    private final UserSkillEventRepository eventRepository;

    @RabbitListener(queues = RabbitMQConstants.ONBOARDING_COMPLETED_QUEUE)
    public void handleOnboarding(UserOnboardingCompletedEvent event) {
        saveSkillEvent(
                event.getUserId(),
                SkillEventSource.ONBOARDING,
                event.getSelectedSkills()
        );
    }


    private void saveSkillEvent(UUID userId, SkillEventSource source, List<UserOnboardingCompletedEvent.SkillSelectionData> incomingSkills) {

        UserSkillEvent skillEvent = new UserSkillEvent();
        skillEvent.setUserId(userId);
        skillEvent.setSource(source);
        skillEvent.setOccurredAt(LocalDateTime.now());

        Set<UserSkillSelection> selections = incomingSkills.stream()
                .map(data -> {
                    UserSkillSelection s = new UserSkillSelection();
                    s.setSkillId(data.getSkillId());
                    s.setSkillName(data.getSkillName());
                    s.setInitialClaimLevel(data.getDifficultyLevel());
                    s.setSelectedLevel(data.getDifficultyLevel());
                    s.setSupportedTaskTypes(data.getSupportedTaskTypes());
                    s.setEvent(skillEvent);
                    return s;
                }).collect(Collectors.toSet());

        skillEvent.setSelections(selections);
        eventRepository.save(skillEvent);

        log.info("Persisted {} skills for user {} via source {}",
                selections.size(), userId, source);
    }
}
