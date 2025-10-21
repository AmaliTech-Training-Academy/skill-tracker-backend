package com.amalitech.user.service.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailUtil {

    private final JavaMailSender mailSender;

    @Autowired
    public EmailUtil(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends a simple email message to a recipient.
     *
     * @param to Recipient's email address
     * @param subject Subject of the email
     * @param message Body of the email
     */
    public void sendEmail(String to, String subject, String message) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setTo(to);
        mail.setSubject(subject);
        mail.setText(message);
        mail.setFrom("no-reply@campusmarket.com"); // Replace with your sender email

        mailSender.send(mail);
    }
}