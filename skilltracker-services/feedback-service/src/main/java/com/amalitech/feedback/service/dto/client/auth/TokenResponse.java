package com.amalitech.feedback.service.dto.client.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO representing the response from the auth server's token endpoint.
 */
@Data
public class TokenResponse {
    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("expires_in")
    private long expiresIn;

    private String scope;
}
