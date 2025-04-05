package com.example.fileuploadservice.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Backoff;

@Configuration
@EnableRetry // Enable Spring Retry
public class MongoConfig {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Bean
    @Retryable(value = { IllegalArgumentException.class }, maxAttempts = 5, backoff = @Backoff(delay = 2000))
    public MongoClient mongoClient() {
        return MongoClients.create(mongoUri);
    }

    @Recover
    public MongoClient recover(IllegalArgumentException e) {
        System.err.println("Failed to create MongoClient after retries: " + e.getMessage());
        // Handle recovery logic, e.g., return a default client or throw an exception
        throw new RuntimeException("Could not connect to MongoDB", e);
    }
} 