package com.dataorchestrate.processing.config;

import com.dataorchestrate.processing.kafka.FileDeletionConsumer;
import com.dataorchestrate.processing.kafka.ProcessingRequestConsumer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import static org.mockito.Mockito.mock;

@Configuration
@Profile("test")
@EnableAutoConfiguration(exclude = KafkaAutoConfiguration.class)
public class TestConfig {
    
    @Bean
    @Primary
    public ConsumerFactory<String, String> consumerFactory() {
        return mock(ConsumerFactory.class);
    }
    
    @Bean
    @Primary
    public ProducerFactory<String, String> producerFactory() {
        return mock(ProducerFactory.class);
    }
    
    @Bean
    @Primary
    public KafkaTemplate<String, String> kafkaTemplate() {
        return mock(KafkaTemplate.class);
    }
    
    @Bean
    @Primary
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        return mock(ConcurrentKafkaListenerContainerFactory.class);
    }
    
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
    
    // Mock Kafka consumers to prevent them from trying to connect to Kafka
    @Bean
    @Primary
    public FileDeletionConsumer fileDeletionConsumer() {
        return mock(FileDeletionConsumer.class);
    }
    
    @Bean
    @Primary
    public ProcessingRequestConsumer processingRequestConsumer() {
        return mock(ProcessingRequestConsumer.class);
    }
}
