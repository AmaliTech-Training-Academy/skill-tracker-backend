package com.amalitech.feedback.service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for Swagger/OpenAPI documentation.
 */
@Configuration
public class SwaggerConfig {

    /**
     * Configures the OpenAPI documentation for the Skill Tracker Feedback Service.
     * @return Configured OpenAPI object with API documentation settings
     */
    @Bean
    public OpenAPI feedbackServiceOpenAPI() {
        return new OpenAPI()
                .addServersItem(new Server()
                        .url("http://localhost:8090")
                        .description("Local development server for Feedback Service. In deployed environments, access via the API Gateway."))
                .info(new Info()
                        .title("Skill Tracker - Feedback Service API")
                        .description("Handles evaluation of user submissions and provides feedback for various task types.")
                        .version("v1.0")
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
