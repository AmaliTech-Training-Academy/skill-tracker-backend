package com.amalitech.gateway.config;

import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties.SwaggerUrl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;

import java.util.HashSet;
import java.util.Set;

@Configuration
public class SwaggerConfig {

    /**
     * This bean is marked as @Primary to resolve the conflict with the
     * default SwaggerUiConfigProperties bean provided by the springdoc library.
     * Spring will now inject this bean by default.
     */
    @Bean
    @Lazy(false)
    @Primary
    public SwaggerUiConfigProperties swaggerUiConfigProperties() {
        SwaggerUiConfigProperties configProperties = new SwaggerUiConfigProperties();
        Set<SwaggerUrl> urls = new HashSet<>();

        urls.add(createSwaggerUrl("USER SERVICE", "/v3/api-docs/user-service"));
        urls.add(createSwaggerUrl("TASK SERVICE", "/v3/api-docs/task-service"));

        configProperties.setUrls(urls);
        configProperties.setDefaultModelsExpandDepth(-1);
        return configProperties;
    }

    private SwaggerUrl createSwaggerUrl(String name, String url) {
        SwaggerUrl swaggerUrl = new SwaggerUrl();
        swaggerUrl.setName(name);
        swaggerUrl.setUrl(url);
        return swaggerUrl;
    }
}