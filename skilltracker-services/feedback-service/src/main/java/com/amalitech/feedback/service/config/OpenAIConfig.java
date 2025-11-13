package com.amalitech.feedback.service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for OpenAI API integration with custom headers.
 * 
 * This configuration customizes the OpenAI API client for the AmaliTech AI proxy gateway
 * which requires:
 * - X-Api-Key header (instead of Authorization: Bearer)
 * - Provider header to specify AI provider (openai or anthropic)
 */
@Configuration
public class OpenAIConfig {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    /**
     * Customizes RestClient to add required headers for AmaliTech AI proxy.
     * This interceptor applies to all RestClient instances created by Spring AI.
     */
    @Bean
    @ConditionalOnProperty(prefix = "spring.ai.openai", name = "base-url")
    public RestClientCustomizer openAiRestClientCustomizer() {
        return restClientBuilder -> restClientBuilder
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().remove("Authorization");
                    request.getHeaders().set("X-API-KEY", apiKey);
                    request.getHeaders().set("Provider", "openai");
                    return execution.execute(request, body);
                });
    }
}
