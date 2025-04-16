package com.dataorchestrate.processing.kafka;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Mock implementation of Kafka listeners for testing.
 * This prevents the real Kafka listeners from being activated during tests.
 */
@Component
@Profile("test")
public class MockKafkaListeners {
    
    // This class replaces the real Kafka listeners in test environment
    // No implementation needed as we just want to prevent the real listeners from being used
}
