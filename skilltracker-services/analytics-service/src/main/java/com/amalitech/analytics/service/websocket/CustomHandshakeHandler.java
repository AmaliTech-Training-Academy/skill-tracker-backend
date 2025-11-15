package com.amalitech.analytics.service.websocket;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

@Component
public class CustomHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request,
                                      WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {

        Principal userPrincipal = (Principal) attributes.get("userPrincipal");
        System.out.println("Determined Principal => " + (userPrincipal != null ? userPrincipal.getName() : "null"));
        return userPrincipal;
    }
}
