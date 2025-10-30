package com.amalitech.notification.service.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.security.Principal;

/**
 * A channel interceptor that associates the authenticated user with the WebSocket session.
 * This is crucial for sending messages to specific users.
 */
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    /**
     * Intercepts messages before they are sent to a channel.
     * If the message is a STOMP CONNECT command, it retrieves the authenticated user
     * from the SecurityContext and sets it on the STOMP session.
     * @param message The message being sent.
     * @param channel The channel to which the message is being sent.
     * @return The modified message, or the original message if no modification was needed.
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null && authentication.isAuthenticated()) {
                String userId = authentication.getName();
                accessor.setUser(new Principal() {
                    @Override
                    public String getName() {
                        return userId;
                    }
                });
            }
        }

        return message;
    }
}
