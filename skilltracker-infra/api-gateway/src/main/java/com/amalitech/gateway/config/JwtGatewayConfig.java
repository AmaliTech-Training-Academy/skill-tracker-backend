package com.amalitech.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.jwt")
public class JwtGatewayConfig {
    private String secret;
    private long expirationMs;

    public String getSecret() {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "JWT secret must be at least 32 characters long. Current length: " +
                            (secret != null ? secret.length() : "null"));
        }
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public void setExpirationMs(long expirationMs) {
        this.expirationMs = expirationMs;
    }
}
