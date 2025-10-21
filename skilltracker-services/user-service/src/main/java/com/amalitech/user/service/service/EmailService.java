package com.amalitech.user.service.service;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;



/**
 * Service interface for handling email-related operations.
 */
@Service
public interface EmailService {

    /**
     * Sends a password reset email to the specified recipient.
     *
     * @param to        recipient's email address
     * @param resetLink the password reset link
     */
    void sendResetEmail(String to, String resetLink);
}