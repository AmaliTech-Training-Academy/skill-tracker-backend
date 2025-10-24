package com.amalitech.user.service.config;

import com.sendgrid.SendGrid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Configuration class for setting up SendGrid email service integration.
 *
 * <p>This class provides Spring configuration for the SendGrid client, which is used
 * for sending transactional emails through the SendGrid API. The configuration
 * retrieves the API key from application properties and creates a singleton
 * SendGrid bean that can be autowired throughout the application.</p>
 *
 * @see SendGrid
 * @see Configuration
 */
@Configuration
public class SendGridConfig {

    @Value("${sendgrid.api.key}")
    private String sendGridApiKey;

    /**
     * Creates and configures a SendGrid client bean for email service operations.
     *
     * <p>This method initializes the SendGrid client with the provided API key
     * and performs validation to ensure the API key is properly configured before
     * creating the bean. The bean is managed by Spring's application context and
     * can be autowired into other components that require email functionality.</p>
     * @see SendGrid
     * @see Bean
     */
    @Bean
    public SendGrid sendGridClient() {
        if (sendGridApiKey == null || sendGridApiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("SendGrid API key is not configured. Please set 'sendgrid.api.key' in your properties.");
        }

        return new SendGrid(sendGridApiKey);
    }
}
