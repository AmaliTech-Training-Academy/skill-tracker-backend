package com.amalitech.task.service.events;

import com.amalitech.task.service.dto.events.SkillEventDTO;
import com.amalitech.task.service.dto.events.UserEventDTO;
import com.amalitech.task.service.model.view.SkillView;
import com.amalitech.task.service.model.view.UserView;
import com.amalitech.task.service.repository.SkillViewRepository;
import com.amalitech.task.service.repository.UserViewRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class DataSyncConsumer {

    private final UserViewRepository userViewRepository;
    private final SkillViewRepository skillViewRepository;

    /**
     * Listens for user-related events (from user-service).
     * This queue name must match the binding in your RabbitMQConfig.
     */
    @RabbitListener(queues = "user.events.task_service.q")
    @Transactional
    public void handleUserEvent(UserEventDTO userEvent) {
        try {
            log.info("Processing user event for user ID: {}", userEvent.id());

            UserView userView = UserView.builder()
                    .id(userEvent.id())
                    .fullName(userEvent.fullName())
                    .email(userEvent.email())
                    .role(userEvent.role())
                    .build();

            userViewRepository.save(userView);

        } catch (Exception e) {
            log.error("Failed to process user event for user ID: {}", userEvent.id(), e);
        }
    }

    /**
     * Listens for skill-related events (from user-service).
     */
    @RabbitListener(queues = "skill.events.task_service.q")
    @Transactional
    public void handleSkillEvent(SkillEventDTO skillEvent) {
        try {
            log.info("Processing skill event for skill ID: {}", skillEvent.id());

            SkillView skillView = SkillView.builder()
                    .id(skillEvent.id())
                    .name(skillEvent.name())
                    .description(skillEvent.description())
                    .build();

            skillViewRepository.save(skillView);

        } catch (Exception e) {
            log.error("Failed to process skill event for skill ID: {}", skillEvent.id(), e);
        }
    }
}