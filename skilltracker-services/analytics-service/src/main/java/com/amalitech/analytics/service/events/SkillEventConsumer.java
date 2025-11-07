package com.amalitech.analytics.service.events;

import com.amalitech.analytics.service.model.SkillSnapShot;
import com.amalitech.analytics.service.repository.SkillSnapshotRepository;
import com.amalitech.common.event.events.RabbitMQConstants;
import com.amalitech.common.event.events.SkillEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Message consumer responsible for handling incoming {@link SkillEvent} messages
 * from RabbitMQ and synchronizing the analytics read model accordingly.
 * <p>
 * This component ensures that the analytics microservice maintains an up-to-date
 * snapshot of all skills whenever a skill is created, updated, or deleted in
 * the primary domain service.
 * </p>
 *
 * <p><strong>Event Processing Flow:</strong></p>
 * <ol>
 *   <li>A domain service publishes a {@link SkillEvent} to {@code skill.exchange}.</li>
 *   <li>The event is routed to {@code skill.queue}.</li>
 *   <li>This consumer receives the event and updates the local {@link SkillSnapShot} table.</li>
 * </ol>
 *
 * <p>The listener uses the {@code rabbitListenerContainerFactory} bean to handle
 * message conversion and deserialization (configured to use JSON in {@code RabbitConfig}).</p>
 *
 * @see SkillEvent
 * @see SkillSnapShot
 * @see SkillSnapshotRepository
 * @see com.amalitech.analytics.service.config.RabbitConfig
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RabbitListener(
        queues = RabbitMQConstants.SKILL_QUEUE,
        containerFactory = "rabbitListenerContainerFactory"
)
public class SkillEventConsumer {

    private final SkillSnapshotRepository repository;

    /**
     * Handles incoming {@link SkillEvent} messages.
     * <p>
     * Based on the {@code eventType}, the method performs one of the following:
     * <ul>
     *   <li>Upserts skill data into the analytics snapshot table on creation or update.</li>
     *   <li>Deletes the corresponding record on skill deletion.</li>
     *   <li>Logs and ignores unsupported event types.</li>
     * </ul>
     * </p>
     *
     * @param event the {@link SkillEvent} received from the queue
     */
    @RabbitListener(queues = RabbitMQConstants.SKILL_QUEUE)
    public void handleSkillEvent(SkillEvent event) {
        log.info("Received Skill Event: {}", event);

        switch (event.getEventType()) {
            case SKILL_CREATED, SKILL_UPDATED -> upsert(event);
            case SKILL_DELETED -> repository.deleteById(event.getSkillId());
            default -> log.info("Ignored unsupported SkillEvent type: {}", event.getEventType());
        }
    }

    /**
     * Inserts or updates a {@link SkillSnapShot} entity based on the incoming event.
     * <p>
     * The snapshot is refreshed with the latest metadata, category, XP map,
     * and timestamp, ensuring eventual consistency with the primary skill service.
     * </p>
     *
     * @param event the {@link SkillEvent} containing updated skill data
     */
    private void upsert(SkillEvent event) {
        SkillSnapShot snapshot = repository.findById(event.getSkillId())
                .orElse(new SkillSnapShot());

        snapshot.setId(event.getSkillId());
        snapshot.setName(event.getName());
        snapshot.setCategory(event.getCategory());
        snapshot.setLevelXpMap(event.getLevelXpMap());
        snapshot.setLastSyncedAt(LocalDateTime.now());

        repository.save(snapshot);
        log.info("Skill snapshot updated for skillId: {}", event.getSkillId());
    }
}


