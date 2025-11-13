package com.amalitech.task.service.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.retry.support.RetryTemplate;

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


    /**
     * Defines the primary, "workhorse" model (gpt-4o-mini).
     * This is the default bean injected when a simple @Autowired ChatModel is requested.
     * It's optimized for speed and low cost (e.g., real-time feedback, simple lookups).
     */
    @Bean
    @Primary
    @Qualifier("workhorseChatModel")
    public ChatModel workhorseChatModel(
            OpenAiApi openAiApi,
            ToolCallingManager toolCallingManager,
            RetryTemplate retryTemplate,
            ObservationRegistry observationRegistry
    ) {
        var options = OpenAiChatOptions.builder()
                .model("gpt-4o-mini")
                .temperature(0.7)
                .build();

        return new OpenAiChatModel(
                openAiApi,
                options,
                toolCallingManager,
                retryTemplate,
                observationRegistry
        );
    }

    /**
     * Defines the "flagship" model (gpt-4o).
     * This bean must be injected using @Qualifier("flagshipChatModel").
     * It's optimized for high-quality, complex generation (e.g., task generation).
     */
    @Bean
    @Qualifier("flagshipChatModel")
    public ChatModel flagshipChatModel(
            OpenAiApi openAiApi,
            ToolCallingManager toolCallingManager,
            RetryTemplate retryTemplate,
            ObservationRegistry observationRegistry
    ) {
        var options = OpenAiChatOptions.builder()
                .model("gpt-4o")
                .temperature(0.5)
                .build();

        return new OpenAiChatModel(
                openAiApi,
                options,
                toolCallingManager,
                retryTemplate,
                observationRegistry
        );
    }
}
