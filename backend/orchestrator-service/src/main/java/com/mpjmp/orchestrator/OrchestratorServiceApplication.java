package com.mpjmp.orchestrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.mpjmp.orchestrator.config.FileStorageProperties;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableConfigurationProperties({FileStorageProperties.class})
@EnableKafka
public class OrchestratorServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrchestratorServiceApplication.class, args);
    }
} 