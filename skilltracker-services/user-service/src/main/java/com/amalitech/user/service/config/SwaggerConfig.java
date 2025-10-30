package com.amalitech.user.service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for Swagger/OpenAPI documentation.
 * Configures the API documentation that will be automatically generated
 * and available at /swagger-ui.html when the application is running.
 *
 * @see "OpenAPI Specification at https://swagger.io/specification/"
 */
@Configuration
public class SwaggerConfig {

    /**
     * Configures the OpenAPI documentation for the Skill Tracker User Service.
     * Includes server information, API metadata, and license details.
     *
     * @return Configured OpenAPI object with API documentation settings
     */
    @Bean
    public OpenAPI userServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Skill Tracker - User Service API")
                        .description("Handles user registration, authentication, profile management, and other user-related operations for the Skill Tracker platform.")
                        .version("v1.0")
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}