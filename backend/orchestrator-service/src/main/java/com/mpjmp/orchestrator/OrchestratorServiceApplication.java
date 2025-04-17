package com.mpjmp.orchestrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.mpjmp.orchestrator.config.FileStorageProperties;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication(scanBasePackages = {"com.mpjmp.orchestrator", "com.dataorchestrate.common"})
@EnableConfigurationProperties({FileStorageProperties.class})
@EnableKafka
@EnableMongoRepositories(basePackages = {"com.mpjmp.orchestrator.repository", "com.dataorchestrate.common"})
public class OrchestratorServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrchestratorServiceApplication.class, args);
    }
}