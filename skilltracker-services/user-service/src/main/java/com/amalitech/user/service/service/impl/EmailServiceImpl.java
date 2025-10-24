package com.amalitech.user.service.service.impl;

import com.amalitech.user.service.service.EmailService;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Implementation of {@link EmailService} for sending emails using SendGrid API.
 *
 * <p>This service provides functionality for sending various types of emails,
 * including password reset notifications, through the SendGrid email delivery service.
 * It constructs and sends MIME messages using the SendGrid Java client library.</p>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li>Sends transactional emails via SendGrid REST API</li>
 *   <li>Handles email sending errors with proper logging</li>
 *   <li>Provides a specialized method for password reset emails</li>
 *   <li>Uses configured sender email from application properties</li>
 * </ul>
 * </p>
 * @see EmailService
 * @see SendGrid
 */
@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final SendGrid sendGrid;
    private final String fromEmail;

    public EmailServiceImpl(SendGrid sendGrid, @Value("${sendgrid.from.email}") String fromEmail) {
        this.sendGrid = sendGrid;
        this.fromEmail = fromEmail;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendEmail(String toEmail, String subject, String body, String from) {

        Email fromSender = new Email(this.fromEmail);
        Email toRecipient = new Email(toEmail);

        Content content = new Content("text/plain", body);

        Mail mail = new Mail(fromSender, subject, toRecipient, content);

        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sendGrid.api(request);

            log.info("SendGrid email request sent to {}. Status Code: {}", toEmail, response.getStatusCode());
            if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
                log.error("Failed to send email via SendGrid. Body: {}", response.getBody());
                throw new RuntimeException("Failed to send email: " + response.getBody());
            }
        } catch (IOException ex) {
            log.error("Error sending email to {}: {}", toEmail, ex.getMessage());
            throw new RuntimeException("Error sending email", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendResetEmail(String to, String resetLink) {

        String subject = "Your SkillBoost Password Reset";
        String body = "Click here to reset your password: " + resetLink;

        this.sendEmail(to, subject, body, this.fromEmail);
    }
}