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

/**
 * Service component responsible for consuming asynchronous events from the message queue (RabbitMQ)
 * to maintain a synchronized, localized read-model (view) of essential data (Users and Skills)
 * required by the Task Service.
 * <p>
 * This pattern helps decouple services and allows the Task Service to query data locally
 * without making synchronous REST calls to the source services (e.g., User Service).
 * The transactions ensure data consistency during the write operation.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DataSyncConsumer {

    private final UserViewRepository userViewRepository;
    private final SkillViewRepository skillViewRepository;

    /**
     * Listens for user-related events (e.g., USER_CREATED, USER_UPDATED) and updates the
     * local {@link UserView} data store.
     * <p>
     * The method is bound to the {@code user.events.task_service.q} queue and executes
     * within a transaction to ensure that the user view is persisted atomically.
     *
     * @param userEvent The {@link UserEventDTO} containing the user data to be synchronized.
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
     * Listens for skill-related events (e.g., SKILL_CREATED, SKILL_UPDATED) and updates the
     * local {@link SkillView} data store.
     * <p>
     * The method is bound to the {@code skill.events.task_service.q} queue and executes
     * within a transaction to ensure the skill view is persisted atomically.
     *
     * @param skillEvent The {@link SkillEventDTO} containing the skill data to be synchronized.
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