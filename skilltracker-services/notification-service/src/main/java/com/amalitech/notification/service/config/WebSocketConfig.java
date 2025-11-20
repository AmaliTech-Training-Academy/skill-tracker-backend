package com.amalitech.notification.service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

/**
 * Configures WebSocket and STOMP messaging for the notification service.
 *
 * This configuration uses a STOMP Broker Relay (RabbitMQ) to enable a stateless,
 * scalable, horizontally distributable messaging system.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final HeaderHandshakeInterceptor headerHandshakeInterceptor;
    private final CustomHandshakeHandler customHandshakeHandler;

    @Value("${stomp.relay.host}")
    private String relayHost;

    @Value("${stomp.relay.port:61613}")
    private int relayPort;

    @Value("${stomp.relay.system-username}")
    private String clientLogin;

    @Value("${stomp.relay.system-password}")
    private String clientPasscode;

    public WebSocketConfig(HeaderHandshakeInterceptor headerHandshakeInterceptor,
                           CustomHandshakeHandler customHandshakeHandler) {
        this.headerHandshakeInterceptor = headerHandshakeInterceptor;
        this.customHandshakeHandler = customHandshakeHandler;
    }

    /**
     * Configures the message broker.
     *
     * With 'enableStompBrokerRelay'.
     * This points our WebSocket connections to RabbitMQ, which acts as the
     * central, scalable broker.
     *
     * @param config The registry for message broker configuration.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");

        config.enableStompBrokerRelay("/queue", "/topic")
                .setRelayHost(relayHost)
                .setRelayPort(relayPort)
                .setClientLogin(clientLogin)
                .setClientPasscode(clientPasscode)
                .setSystemLogin(clientLogin)
                .setSystemPasscode(clientPasscode)

                .setSystemHeartbeatSendInterval(20000)
                .setSystemHeartbeatReceiveInterval(20000);
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
                .addInterceptors(headerHandshakeInterceptor, new HttpSessionHandshakeInterceptor())
                .setHandshakeHandler(customHandshakeHandler)
                .setAllowedOriginPatterns("*");
    }
}
