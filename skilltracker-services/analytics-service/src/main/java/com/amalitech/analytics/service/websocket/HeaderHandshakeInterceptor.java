package com.amalitech.analytics.service.websocket;

import com.amalitech.analytics.service.security.filter.JwtAuthenticationFilter;
import com.amalitech.analytics.service.security.util.JwtUtil;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class HeaderHandshakeInterceptor implements HandshakeInterceptor {
    private static final Logger log = LoggerFactory.getLogger(HandshakeInterceptor.class);

    private final JwtUtil jwtUtil;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {

        if (request instanceof ServletServerHttpRequest servletRequest) {

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userId = authentication.getName().toString();
            if (userId != null) {
                attributes.put("userPrincipal", new StompPrincipal(userId));
                log.info("WebSocket Principal Set: {}", userId);
            }
        }
        log.info("Attributes after set: {}", attributes);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, @Nullable Exception exception) { }
}


