package com.amalitech.task.service.config;

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
     * Includes server information, API metadata, and license details.
     *
     * @return Configured OpenAPI object with API documentation settings
     */
    @Bean
    public OpenAPI taskServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Skill Tracker - Task Service API")
                        .description("Handles AI task generation, execution and other task-related operations for the Skill Tracker platform.")
                        .version("v1.0")
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}