package com.example.storage_service;

import com.example.storage_service.DeviceRegistryService;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class StorageServiceApplication {

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
        SpringApplication.run(StorageServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner run(DeviceRegistryService deviceRegistryService) {
        return args -> deviceRegistryService.registerDevice();
    }
}
