package com.example.orchestrator_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
// @EnableKafka removed for HTTP-based orchestration
public class OrchestratorServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrchestratorServiceApplication.class, args);
    }
}
