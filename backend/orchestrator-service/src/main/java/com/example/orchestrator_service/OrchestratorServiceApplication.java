package com.example.orchestrator_service;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OrchestratorServiceApplication {
    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure()
            .ignoreIfMissing()
            .load();
        
        // Validate MongoDB URI
        String mongoUri = dotenv.get("MONGODB_URI");
        if (mongoUri == null || !(mongoUri.startsWith("mongodb://") || mongoUri.startsWith("mongodb+srv://"))) {
            throw new IllegalArgumentException("Invalid MongoDB URI in .env file");
        }
        
        System.setProperty("MONGODB_URI", mongoUri);
        SpringApplication.run(OrchestratorServiceApplication.class, args);
    }
}
