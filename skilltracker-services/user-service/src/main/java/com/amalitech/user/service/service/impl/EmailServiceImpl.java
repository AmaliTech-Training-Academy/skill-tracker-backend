package com.amalitech.user.service.service.impl;
import com.amalitech.user.service.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Service for sending emails, such as password reset notifications.
 * <p>
 * Uses Spring's {@link JavaMailSender} to construct and send MIME messages.
 * </p>
 */
@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;

    /**
     * Constructs the EmailService with the given {@link JavaMailSender}.
     *
     * @param mailSender the JavaMailSender used to send emails
     */
    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends a password reset email to the specified recipient with a reset link.
     * <p>
     * The email contains an HTML link that the user can click to reset their password.
     * </p>
     *
     * @param to the recipient's email address
     * @param resetLink the password reset link to include in the email
     * @throws RuntimeException if sending the email fails
     */
    public void sendResetEmail(String to, String resetLink) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setSubject("Password Reset Request");
            helper.setText("<p>Click <a href=\"" + resetLink + "\">here</a> to reset your password.</p>", true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }


    public void sendEmail(String to, String subject, String message, String sender){
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setTo(to);
        mail.setSubject(subject);
        mail.setText(message);
        mail.setFrom(sender);

        mailSender.send(mail);
    }
}