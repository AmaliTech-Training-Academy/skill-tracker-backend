package com.amalitech.feedback.service.config;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * Configures "smart" WebClient beans for all external service communication.
 *
 * This centralizes timeout, base URL, and authentication logic for:
 * 1. Task Service (Internal)
 * 2. Judge0 API (Internal/Self-Hosted)
 * 3. DeepSeek AI (External)
 *
 * To use in a service:
 * @Qualifier("taskServiceWebClient")
 * private final WebClient taskServiceClient;
 *
 * @Qualifier("judge0ApiWebClient")
 * private final WebClient judge0ApiClient;
 *
 * @Qualifier("deepseekApiWebClient")
 * private final WebClient deepseekApiClient;
 */
@Configuration
public class WebClientConfig {

    @Value("${client.connect-timeout-ms:5000}")
    private int globalConnectTimeoutMs;

    // --- Judge0 API Properties ---
    @Value("${client.judge0-api.base-url}")
    private String judge0ApiBaseUrl;
    @Value("${client.judge0-api.response-timeout-ms:30000}")
    private int judge0ApiResponseTimeoutMs;

    // --- DeepSeek AI Properties ---
    @Value("${client.deepseek-api.base-url}")
    private String deepseekApiBaseUrl;
    @Value("${client.deepseek-api.api-key}")
    private String deepseekApiKey;
    @Value("${client.deepseek-api.response-timeout-ms:45000}")
    private int deepseekApiResponseTimeoutMs;

    /**
     * Creates a pre-configured HTTP client with our standard timeouts.
     * @param responseTimeoutMs How long to wait for a response *after* connecting.
     * @return A configured Reactor Netty HttpClient.
     */
    private HttpClient createHttpClient(int responseTimeoutMs) {
        return HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, globalConnectTimeoutMs)
                .responseTimeout(Duration.ofMillis(responseTimeoutMs));
    }

    /**
     * Creates the WebClient for communicating with the self-hosted Judge0 API.
     */
    @Bean("judge0ApiWebClient")
    public WebClient judge0ApiWebClient(WebClient.Builder builder) {
        HttpClient httpClient = createHttpClient(judge0ApiResponseTimeoutMs);

        return builder
                .baseUrl(judge0ApiBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                // Note: Add any auth headers (e.g., X-RapidAPI-Key) here
                // if you put your Judge0 instance behind a gateway.
                .build();
    }

    /**
     * Creates the WebClient for communicating with the external DeepSeek AI API.
     * This bean automatically includes the Bearer token for authentication.
     */
    @Bean("deepseekApiWebClient")
    public WebClient deepseekApiWebClient(WebClient.Builder builder) {
        if (deepseekApiKey == null || deepseekApiKey.isBlank()) {
            throw new IllegalArgumentException("DeepSeek API key is not configured. " +
                    "Please set DEEPSEEK_API_KEY environment variable.");
        }

        HttpClient httpClient = createHttpClient(deepseekApiResponseTimeoutMs);

        return builder
                .baseUrl(deepseekApiBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + deepseekApiKey)
                .build();
    }
}