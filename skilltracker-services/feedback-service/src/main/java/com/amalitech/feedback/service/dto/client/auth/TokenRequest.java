package com.amalitech.feedback.service.dto.client.auth;

import lombok.Builder;
import lombok.Data;

/**
 * DTO for requesting a token using Client Credentials grant.
 * Sent as form urlencoded data.
 */
@Data
@Builder
public class TokenRequest {
    private String grant_type;
    private String client_id;
    private String client_secret;
    private String scope;
}
