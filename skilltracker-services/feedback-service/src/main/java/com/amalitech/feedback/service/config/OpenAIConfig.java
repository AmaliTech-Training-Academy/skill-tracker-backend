package com.amalitech.feedback.service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for OpenAI API integration with custom headers.
 * 
 * This configuration customizes the OpenAI API client to use X-Api-Key header
 * instead of the standard Authorization: Bearer header, as required by the
 * AmaliTech AI proxy gateway.
 */
@Configuration
public class OpenAIConfig {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    /**
     * Customizes RestClient to add X-Api-Key header instead of Authorization.
     * This interceptor applies to all RestClient instances created by Spring AI.
     */
    @Bean
    @ConditionalOnProperty(prefix = "spring.ai.openai", name = "base-url")
    public RestClientCustomizer openAiRestClientCustomizer() {
        return restClientBuilder -> restClientBuilder
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().remove("Authorization");
                    request.getHeaders().set("X-Api-Key", apiKey);
                    return execution.execute(request, body);
                });
    }
}
