package com.amalitech.user.service.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration class for password generation settings.
 * Properties are loaded from application.properties with prefix "app.password"
 */
@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "app.password")
@Validated
public class PasswordConfig {
    @NotBlank
    private String uppercaseLetters;

    @NotBlank
    private String lowercaseLetters;

    @NotBlank
    private String numbers;

    @NotBlank
    private String specialCharacters;

    @Min(8) // Minimum password length
    private int length;

    /**
     * Returns all allowed characters combined for password generation
     */
    public String getAllCharacters() {
        return uppercaseLetters + lowercaseLetters + numbers + specialCharacters;
    }
}
