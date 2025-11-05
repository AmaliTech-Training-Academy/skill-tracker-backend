package com.amalitech.user.service.events;

import com.amalitech.common.event.events.TaskGenerationFailedEvent;
import com.amalitech.common.event.events.TaskGenerationSucceededEvent;
import com.amalitech.user.service.model.User;
import com.amalitech.user.service.model.enums.UserState;
import com.amalitech.user.service.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class TaskGenerationEventConsumer {

    private final UserRepository userRepository;

    /**
     * This is the "Success" part of the saga.
     * The user's state is finalized to ONBOARDED.
     */
    @RabbitListener(queues = "task.generation.success.user_service.q")
    @Transactional
    public void handleTaskGenerationSucceeded(TaskGenerationSucceededEvent event) {
        log.info("Task generation SUCCEEDED for user: {}", event.getUserId());

        User user = userRepository.findById(event.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + event.getUserId()));

        if (user.getState() == UserState.PENDING_TASKS) {
            user.setState(UserState.ONBOARDED);
            userRepository.save(user);
            log.info("User {} state finalized to ONBOARDED.", user.getId());
        } else {
            log.warn("Received task success event for user {} in unexpected state: {}",
                    user.getId(), user.getState());
        }
    }

    /**
     * This is the "Compensating Transaction" (Rollback).
     * The user's state is reset, allowing them to try onboarding again.
     */
    @RabbitListener(queues = "task.generation.failed.user_service.q")
    @Transactional
    public void handleTaskGenerationFailed(TaskGenerationFailedEvent event) {
        log.warn("Task generation FAILED for user: {}. Reason: {}",
                event.getUserId(), event.getErrorMessage());

        User user = userRepository.findById(event.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + event.getUserId()));

        if (user.getState() == UserState.PENDING_TASKS) {
            user.setState(UserState.REGISTERED);
            userRepository.save(user);
            log.warn("User {} state has been ROLLED BACK to INCOMPLETE.", user.getId());
        } else {
            log.warn("Received task failure event for user {} in unexpected state: {}",
                    user.getId(), user.getState());
        }
    }
}