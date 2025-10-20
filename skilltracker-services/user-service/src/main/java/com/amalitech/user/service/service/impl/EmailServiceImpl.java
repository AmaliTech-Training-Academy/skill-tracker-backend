package com.amalitech.user.service.service.impl;

import com.amalitech.user.service.service.EmailService;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementation of the {@link EmailService} interface for sending emails.
 * <p>
 * Uses Spring's {@link JavaMailSender} to construct and send MIME messages.
 * </p>
 */
@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;

    /**
     * Constructs the EmailServiceImpl with the given {@link JavaMailSender}.
     *
     * @param mailSender the JavaMailSender used to send emails
     */
    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendResetEmail(String to, String resetLink) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setSubject("Password Reset Request");
            helper.setText("<p>Click <a href=\"" + resetLink + "\">here</a> to reset your password.</p>", true);
            mailSender.send(message);
            log.info("Password reset email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send password reset email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
