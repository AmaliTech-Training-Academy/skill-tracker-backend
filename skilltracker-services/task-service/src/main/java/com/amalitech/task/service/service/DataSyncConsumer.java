package com.amalitech.task.service.service;

import com.amalitech.common.event.events.SkillEvent;
import com.amalitech.task.service.model.view.SkillView;
import com.amalitech.task.service.repository.SkillViewRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;

/**
 * Service component responsible for consuming asynchronous skill events from the message queue (RabbitMQ)
 * to maintain a synchronized, localized read-model (view) of skill data required by the Task Service.
 * <p>
 * This pattern helps decouple services and allows the Task Service to query skill data locally
 * without making synchronous REST calls to the User Service.
 * The transactions ensure data consistency during the write operation.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DataSyncConsumer {

    private final SkillViewRepository skillViewRepository;



    /**
     * Listens for skill-related events (SKILL_CREATED, SKILL_UPDATED, SKILL_DELETED) and updates the
     * local {@link SkillView} data store accordingly.
     * <p>
     * The method is bound to the {@code skill.events.task_service.q} queue and executes
     * within a transaction to ensure the skill view is persisted atomically.
     *
     * @param skillEvent The {@link SkillEvent} containing the skill data and operation type.
     */
    @RabbitListener(queues = "skill.events.task_service.q")
    @Transactional
    public void handleSkillEvent(SkillEvent skillEvent) {
        try {
            log.info("Processing {} event for skill ID: {}", skillEvent.getEventType(), skillEvent.getSkillId());

            if (skillEvent.getEventType() == SkillEvent.EventType.SKILL_DELETED) {
                skillViewRepository.deleteById(skillEvent.getSkillId());
                log.info("Deleted skill view for skill: {}", skillEvent.getSkillId());
                return;
            }

            SkillView skillView = SkillView.builder()
                    .id(skillEvent.getSkillId())
                    .name(skillEvent.getName())
                    .description(skillEvent.getDescription())
                    .supportedTaskTypes(new HashSet<>(skillEvent.getSupportedTaskTypes()))
                    .build();

            skillViewRepository.save(skillView);
            log.info("Synchronized skill view for skill: {}", skillEvent.getSkillId());

        } catch (Exception e) {
            log.error("Failed to process {} event for skill ID: {}", skillEvent.getEventType(), skillEvent.getSkillId(), e);
        }
    }
}