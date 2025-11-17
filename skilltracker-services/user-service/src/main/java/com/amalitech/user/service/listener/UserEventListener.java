package com.amalitech.user.service.listener;

import com.amalitech.user.service.events.AdminCreatedUserEvent;
import com.amalitech.user.service.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventListener {

    private final EmailService emailService;

    /**
     * Listens for the AdminCreatedUserEvent and sends the welcome email.
     *
     * @TransactionalEventListener ensures this runs AFTER the user creation
     * transaction has successfully committed, fixing the 503/LazyInit error.
     *
     * @param event The event containing the new user's details.
     */
    @TransactionalEventListener
    public void onAdminCreatedUser(AdminCreatedUserEvent event) {
        log.info("Transaction committed for user {}. Sending async welcome email.", event.userId());
        try {

            emailService.sendAdminCreatedUserEmail(
                    event.email(),
                    event.rawPassword(),
                    event.adminEmail(),
                    event.loginUrl()
            );
        } catch (Exception e) {
            log.error("Failed to send async email to user: {}", event.email(), e);
        }
    }
}