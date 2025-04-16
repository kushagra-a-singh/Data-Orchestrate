package com.dataorchestrate.processing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication(scanBasePackages = {"com.dataorchestrate.processing", "com.dataorchestrate.common"})
@EnableKafka
@EnableRetry
@EnableMongoRepositories(basePackages = "com.dataorchestrate.processing.repository")
public class ProcessingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProcessingServiceApplication.class, args);
    }
} 