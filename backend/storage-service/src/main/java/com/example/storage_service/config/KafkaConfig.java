package com.example.storage_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${kafka.topic.file-storage}")
    private String fileStorageTopic;

    @Value("${kafka.topic.file-status}")
    private String fileStatusTopic;

    @Value("${kafka.topic.file-replication}")
    private String fileReplicationTopic;

    @Bean
    public NewTopic fileStorageTopic() {
        return TopicBuilder.name(fileStorageTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic fileStatusTopic() {
        return TopicBuilder.name(fileStatusTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic fileReplicationTopic() {
        return TopicBuilder.name(fileReplicationTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }
} 