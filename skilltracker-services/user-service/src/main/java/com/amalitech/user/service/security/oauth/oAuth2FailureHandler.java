package com.amalitech.user.service.security.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;


import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class oAuth2FailureHandler implements AuthenticationFailureHandler {
    private static final Logger log = LoggerFactory.getLogger(oAuth2FailureHandler.class);

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {

        String errorMessage = switch (exception.getClass().getSimpleName()) {
            case "OAuth2AuthorizationException" -> "Authorization failed with provider";
            case "OAuth2AuthenticationException" -> "Invalid OAuth2 authentication response";
            case "OAuth2AccessDeniedException" -> "Access denied by OAuth2 provider";
            default -> "Unexpected authentication error";
        };


        log.error("OAuth2 Failure: {}", exception.getMessage(), exception);

        response.sendRedirect(frontendUrl + "/auth/error?reason=" + URLEncoder.encode(errorMessage, StandardCharsets.UTF_8));
    }
}

