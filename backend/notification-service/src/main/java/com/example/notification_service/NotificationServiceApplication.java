package com.example.notification_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
@EnableRetry
public class NotificationServiceApplication {
    static {
        Dotenv dotenv = Dotenv.configure()
            .directory("../../") // points to project root
            .ignoreIfMissing()
            .load();
        dotenv.entries().forEach(entry -> {
            if (System.getProperty(entry.getKey()) == null) {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        });
        // Map NOTIFICATION_PORT to server.port if present
        String notifPort = System.getProperty("NOTIFICATION_PORT");
        if (notifPort != null && System.getProperty("server.port") == null) {
            System.setProperty("server.port", notifPort);
        }
    }
    public static void main(String[] args) {
        System.out.println("[NotificationServiceApplication] Starting on port: " + System.getProperty("server.port", "8080"));
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
