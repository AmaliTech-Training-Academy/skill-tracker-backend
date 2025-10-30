package com.amalitech.notification.service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configures WebSocket and STOMP messaging for the notification service.
 * This class sets up the message broker, application destination prefixes,
 * and the WebSocket endpoint.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    public WebSocketConfig(WebSocketAuthInterceptor webSocketAuthInterceptor) {
        this.webSocketAuthInterceptor = webSocketAuthInterceptor;
    }

    /**
     * Configures the message broker.
     * It enables a simple in-memory broker for destinations prefixed with "/queue"
     * and sets the application destination prefix to "/app".
     * It also configures the prefix for user-specific destinations.
     * @param config The registry for message broker configuration.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    /**
     * Registers the STOMP endpoint for WebSocket connections.
     * The endpoint is available at "/ws" and is configured with allowed origins
     * and SockJS fallback support.
     * @param registry The registry for STOMP endpoints.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("https://dev.dy006p1vkpl2e.amplifyapp.com", "http://localhost:3000")
                .withSockJS();
    }

    /**
     * Configures the client inbound channel to include the authentication interceptor.
     * This ensures that user information is attached to the session upon connection.
     * @param registration The registration for the client inbound channel.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);
    }
}
