package com.mpjmp.common.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic fileUploadTopic() {
        return new NewTopic("file-upload", 1, (short) 1);
    }

    @Bean
    public NewTopic fileProcessingTopic() {
        return new NewTopic("file-processing", 1, (short) 1);
    }

    @Bean
    public NewTopic fileStorageTopic() {
        return new NewTopic("file-storage", 1, (short) 1);
    }

    @Bean
    public NewTopic notificationTopic() {
        return new NewTopic("notification", 1, (short) 1);
    }
}