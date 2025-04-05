package com.example.orchestrator_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@EmbeddedKafka(partitions = 1, brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"})
@TestPropertySource(properties = {
    "spring.data.mongodb.uri=mongodb://localhost:27017/testdb",
    "spring.data.mongodb.database=testdb",
    "spring.kafka.bootstrap-servers=localhost:9092",
    "spring.kafka.consumer.group-id=test-group"
})
class OrchestratorServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}