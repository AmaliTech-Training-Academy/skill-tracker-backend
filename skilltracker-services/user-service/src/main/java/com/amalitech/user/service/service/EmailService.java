package com.amalitech.user.service.service;

/**
 * Service interface for sending emails, such as password reset notifications.
 * <p>
 * Defines the contract for all email-sending operations within the application.
 * </p>
 */
public interface EmailService {

    /**
     * Sends a password reset email to the specified recipient with a reset link.
     * <p>
     * The email contains an HTML link that the user can click to reset their password.
     * </p>
     *
     * @param to        the recipient's email address
     * @param resetLink the password reset link to include in the email
     * @throws RuntimeException if sending the email fails (e.g., due to MessagingException)
     */
    void sendResetEmail(String to, String resetLink);
}
