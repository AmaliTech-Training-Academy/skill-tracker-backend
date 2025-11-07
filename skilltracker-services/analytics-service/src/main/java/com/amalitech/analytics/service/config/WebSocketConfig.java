package com.amalitech.analytics.service.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;


/**
 * WebSocket configuration for enabling STOMP (Simple Text Oriented Messaging Protocol)
 * messaging over WebSocket connections.
 * <p>
 * This configuration connects the application to an external RabbitMQ STOMP broker relay,
 * allowing scalable, message-driven communication between distributed clients.
 * It defines endpoint mappings, broker relay settings, and routing prefixes for
 * server-to-client and client-to-server communication.
 * </p>
 *
 * <p><strong>Key Features:</strong></p>
 * <ul>
 *   <li>Connects to an external RabbitMQ STOMP relay for high scalability.</li>
 *   <li>Defines prefixes for application destinations and user-specific queues.</li>
 *   <li>Registers a WebSocket handshake endpoint for clients.</li>
 * </ul>
 *
 * <p>
 * The {@link EnableWebSocketMessageBroker} annotation enables message handling backed
 * by a message broker, making it possible to route messages via topics and queues
 * using annotations such as {@code @MessageMapping}.
 * </p>
 *
 * @author
 * @since 1.0
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${stomp.relay.host}")
    private String rabbitStompHost;

    @Value("${stomp.relay.port}")
    private int rabbitStompPort;

    @Value("${stomp.relay.system-username}")
    private String rabbitSystemUsername;

    @Value("${stomp.relay.system-password}")
    private String rabbitSystemPassword;

    /**
     * Configures the message broker for STOMP communication.
     * <p>
     * This method sets up a STOMP broker relay connected to RabbitMQ, defines
     * prefixes for message destinations, and configures user-specific queue routing.
     * </p>
     *
     * @param registry the {@link MessageBrokerRegistry} used to configure broker settings
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableStompBrokerRelay("/queue", "/topic")
                .setRelayHost(rabbitStompHost)
                .setRelayPort(rabbitStompPort)
                .setClientLogin(rabbitSystemUsername)
                .setClientPasscode(rabbitSystemPassword)
                .setSystemLogin(rabbitSystemUsername)
                .setSystemPasscode(rabbitSystemPassword)
                .setUserDestinationBroadcast("/topic/unresolved-user-destination")
                .setUserRegistryBroadcast("/topic/user-registry");

        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * Registers WebSocket endpoints for client connections.
     * <p>
     * The endpoint {@code /ws-analytics} serves as the HTTP entry point
     * for WebSocket handshake requests. Clients connect to this endpoint
     * before subscribing to STOMP topics or queues.
     * </p>
     *
     * <p><strong>Note:</strong> The {@code setAllowedOriginPatterns("*")} setting
     * is for development use only. In production, specify explicit origins
     * to prevent cross-origin security risks.</p>
     *
     * @param registry the {@link StompEndpointRegistry} used to register STOMP endpoints
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-analytics")
                .setAllowedOriginPatterns("*");
    }
}
