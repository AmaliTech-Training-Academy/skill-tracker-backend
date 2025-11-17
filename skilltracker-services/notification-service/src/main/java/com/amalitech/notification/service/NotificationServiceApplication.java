package com.amalitech.notification.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * The main entry point for the Notification Service application.
 * This class initializes and runs the Spring Boot application.
 */
@SpringBootApplication
@EnableAsync
public class NotificationServiceApplication {

    /**
     * The main method that starts the Spring Boot application.
     * @param args Command line arguments passed to the application.
     */
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
