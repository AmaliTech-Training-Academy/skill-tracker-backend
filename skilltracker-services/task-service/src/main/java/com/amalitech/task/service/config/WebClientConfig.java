package com.amalitech.task.service.config;

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
 * Configures "smart" WebClient beans for external service communication.
 *
 * This centralizes timeout, base URL, and authentication logic for:
 * 1. Judge0 API (Internal/Self-Hosted)
 *
 * To use in a service:
 * @Qualifier("judge0ApiWebClient")
 * private final WebClient judge0ApiClient;
 */
@Configuration
public class WebClientConfig {

    @Value("${client.connect-timeout-ms:5000}")
    private int globalConnectTimeoutMs;

    @Value("${client.judge0-api.base-url}")
    private String judge0ApiBaseUrl;
    @Value("${client.judge0-api.response-timeout-ms:30000}")
    private int judge0ApiResponseTimeoutMs;

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
                .build();
    }
}
