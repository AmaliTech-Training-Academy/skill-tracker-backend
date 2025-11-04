package com.amalitech.gateway.config;

import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * This configuration class fixes the issue where Swagger UI tries to send
 * requests directly to downstream microservices instead of the API Gateway.
 *
 * It intercepts all OpenAPI specifications as they are loaded and
 * rewrites the 'servers' block to point all requests to the gateway's
 * relative root path.
 */
@Configuration
public class OpenApiServerRewriter {

    /**
     * This customizer bean is applied to all OpenAPI definitions loaded by springdoc.
     * It replaces the downstream service's server URL (e.g., http://10.255.255.254:8084)
     * with the gateway's relative path.
     *
     * @return A customizer bean.
     */
    @Bean
    public OpenApiCustomizer gatewayServerCustomizer() {
        return openApi -> {
            Server gatewayServer = new Server();
            gatewayServer.setUrl("/");
            gatewayServer.setDescription("API Gateway");

            openApi.setServers(List.of(gatewayServer));
        };
    }
}