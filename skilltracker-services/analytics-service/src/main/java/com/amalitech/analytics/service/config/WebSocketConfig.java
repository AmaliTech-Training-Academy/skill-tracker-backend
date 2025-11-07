package com.amalitech.analytics.service.config;

// File: com.your-app.config.WebSocketConfig.java
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // Inject dedicated STOMP properties
    @Value("${stomp.relay.host}")
    private String rabbitStompHost;

    @Value("${stomp.relay.port}")
    private int rabbitStompPort;

    @Value("${stomp.relay.system-username}")
    private String rabbitSystemUsername;

    @Value("${stomp.relay.system-password}")
    private String rabbitSystemPassword;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {

        // Setting up the scalable, external broker relay
        registry.enableStompBrokerRelay("/queue", "/topic")
                .setRelayHost(rabbitStompHost)
                .setRelayPort(rabbitStompPort)

                // Credentials for your Spring app to connect to RabbitMQ as a STOMP client
                .setClientLogin(rabbitSystemUsername)
                .setClientPasscode(rabbitSystemPassword)

                // Credentials for your Spring app to manage queue subscriptions on behalf of the users
                .setSystemLogin(rabbitSystemUsername)
                .setSystemPasscode(rabbitSystemPassword)

                // Recommended settings for troubleshooting and user management in a distributed setup
                .setUserDestinationBroadcast("/topic/unresolved-user-destination")
                .setUserRegistryBroadcast("/topic/user-registry");

        // Prefix for server-side endpoints (e.g., @MessageMapping("/app/...") )
        registry.setApplicationDestinationPrefixes("/app");
        // Prefix for user-specific queues (e.g., /user/UUID-123/queue/updates)
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // The HTTP endpoint for the initial WebSocket handshake
        registry.addEndpoint("/ws-analytics")
                .setAllowedOriginPatterns("*"); // Configure this securely in production
    }
}