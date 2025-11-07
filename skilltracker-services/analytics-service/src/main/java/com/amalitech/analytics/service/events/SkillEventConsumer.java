package com.amalitech.analytics.service.events;

import com.amalitech.analytics.service.model.SkillSnapShot;
import com.amalitech.analytics.service.repository.SkillSnapshotRepository;
import com.amalitech.common.event.events.RabbitMQConstants;
import com.amalitech.common.event.events.SkillEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static com.amalitech.common.event.events.SkillEvent.EventType.*;



@Slf4j
@Component
@RequiredArgsConstructor
@RabbitListener(
        queues = RabbitMQConstants.SKILL_QUEUE,
        containerFactory = "rabbitListenerContainerFactory"
)
public class SkillEventConsumer {

    private final SkillSnapshotRepository repository;

    @RabbitListener(queues = "skills.queue")
    public void handleSkillEvent(SkillEvent event) {
        log.info("Received Skill Event: {}", event);

        switch (event.getEventType()) {
            case SKILL_CREATED, SKILL_UPDATED -> upsert(event);
            case SKILL_DELETED -> repository.deleteById(event.getSkillId());
            default -> log.info("Event ignored: {}", event.getEventType());
        }
    }

    private void upsert(SkillEvent event) {
        SkillSnapShot snapshot = repository.findById(event.getSkillId())
                .orElse(new SkillSnapShot());
        snapshot.setId(event.getSkillId());
        snapshot.setName(event.getName());
        snapshot.setCategory(event.getCategory());
        snapshot.setLevelXpMap(event.getLevelXpMap());
        snapshot.setLastSyncedAt(LocalDateTime.now());
        repository.save(snapshot);
    }
}

