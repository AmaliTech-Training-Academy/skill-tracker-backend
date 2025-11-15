package com.amalitech.analytics.service.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration class for customizing the Jackson {@link ObjectMapper}.
 * <p>
 * This configuration registers the {@link JavaTimeModule} to ensure proper
 * serialization and deserialization of Java 8 Date and Time API classes
 * (e.g., {@link java.time.LocalDate}, {@link java.time.LocalDateTime}, etc.).
 * <p>
 * The configured {@link ObjectMapper} bean will be managed by Spring's
 * application context, allowing it to be injected wherever JSON processing
 * is required within the application.
 * </p>
 *
 * <p><b>Usage example:</b></p>
 * <pre>{@code
 * @Autowired
 * private ObjectMapper objectMapper;
 * }</pre>
 *
 * @author
 * @since 1.0
 */
@Configuration
public class ObjectMapperConfig {

    /**
     * Creates and configures an {@link ObjectMapper} bean with support for Java 8 time types
     * and tolerance for unknown properties.
     * <p>
     * By registering the {@link JavaTimeModule}, this mapper can correctly handle
     * serialization and deserialization of classes such as {@link java.time.LocalDateTime}.
     * Additionally, disabling {@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES}
     * allows deserialization to ignore unrecognized fields like "principal" in version-mismatched payloads.
     * </p>
     *
     * @return a configured {@link ObjectMapper} instance
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);  // Key addition to ignore unknown fields
        return mapper;
    }

    /**
     * Customizer to apply the same configuration (ignoring unknown properties) to any
     * ObjectMappers built by Spring Boot's Jackson auto-configuration. This ensures
     * consistency, including for internal mappers like those in messaging converters.
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> {
            builder.modules(new JavaTimeModule());
            builder.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        };
    }
}