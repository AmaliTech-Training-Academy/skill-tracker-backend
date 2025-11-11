package com.amalitech.user.service.service.impl;

import com.amalitech.user.service.service.EmailService;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

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
    private final ResourceLoader resourceLoader;

    private String resetTemplate;
    private String adminCreatedUserTemplate;

    public EmailServiceImpl(SendGrid sendGrid,
                            @Value("${sendgrid.from.email}") String fromEmail,
                            ResourceLoader resourceLoader) {
        this.sendGrid = sendGrid;
        this.fromEmail = fromEmail;
        this.resourceLoader = resourceLoader;
    }

    /**
     * This method runs once when the service starts.
     * It loads the HTML template from the classpath and stores it in memory.
     */
    @PostConstruct
    public void loadTemplate() {
        try {
            Resource resource = resourceLoader.getResource("classpath:templates/email-templates/reset-password.html");
            try (InputStream inputStream = resource.getInputStream()) {
                this.resetTemplate = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
                log.info("Successfully loaded 'reset-password.html' template.");
            }

            Resource adminResource = resourceLoader.getResource("classpath:templates/email-templates/admin-created-user.html");
            try (InputStream inputStream = adminResource.getInputStream()) {
                this.adminCreatedUserTemplate = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
                log.info("Successfully loaded 'admin-created-user.html' template.");
            }
        } catch (IOException e) {
            log.error("Failed to load email templates", e);
            throw new RuntimeException("Failed to load email template", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendEmail(String toEmail, String subject, String body, String from) {
        this.send(toEmail, subject, body, "text/plain");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendResetEmail(String to, String resetLink) {
        String subject = "Your SkillBoost Password Reset";

        String body = this.resetTemplate.replace("{{resetLink}}", resetLink);

        this.send(to, subject, body, "text/html");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendAdminCreatedUserEmail(String toEmail, String temporaryPassword, String adminEmail) {
        String subject = "Your SkillBoost Account Created";

        String body = this.adminCreatedUserTemplate
                .replace("{{email}}", toEmail)
                .replace("{{temporaryPassword}}", temporaryPassword)
                .replace("{{adminEmail}}", adminEmail);

        this.send(toEmail, subject, body, "text/html");
    }

    /**
     * Worker method to handle the actual SendGrid API call.
     * It now accepts a dynamic contentType.
     */
    private void send(String toEmail, String subject, String body, String contentType) {
        Email fromSender = new Email(this.fromEmail);
        Email toRecipient = new Email(toEmail);
        Content content = new Content(contentType, body);
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
}