package com.example.orchestrator_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class OrchestratorListener {
    private static final Logger logger = LoggerFactory.getLogger(OrchestratorListener.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Value("${kafka.topic.file-upload}")
    private String fileUploadTopic;

    @Value("${kafka.topic.file-processed}")
    private String fileProcessedTopic;

    @Value("${kafka.topic.file-error}")
    private String fileErrorTopic;

    @KafkaListener(topics = "${kafka.topic.file-upload}", groupId = "${spring.kafka.consumer.group-id}")
    public void listenFileUploads(String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            logger.info("Received file upload message from topic {}: {}", topic, message);
            
            // Parse the message to get file details
            Map<String, String> fileDetails = objectMapper.readValue(message, new com.fasterxml.jackson.core.type.TypeReference<HashMap<String, String>>() {});
            String fileId = fileDetails.get("fileId");
            String filePath = fileDetails.get("filePath");
            String operationType = fileDetails.getOrDefault("operationType", "default");
            
            logger.info("Processing file: ID={}, Path={}, Operation={}", fileId, filePath, operationType);
            
            // Forward the message to the processing service
            kafkaTemplate.send(fileUploadTopic, message);
            
            // Log the orchestration action
            logger.info("Forwarded file processing request for fileId: {}", fileId);
            
        } catch (Exception e) {
            logger.error("Error processing file upload message: {}", e.getMessage(), e);
            
            // Send error message to error topic
            try {
                Map<String, String> errorDetails = new HashMap<>();
                errorDetails.put("error", "Failed to process file upload");
                errorDetails.put("errorMessage", e.getMessage());
                
                String errorMessage = objectMapper.writeValueAsString(errorDetails);
                kafkaTemplate.send(fileErrorTopic, errorMessage);
                logger.info("Sent error message to topic: {}", fileErrorTopic);
            } catch (Exception ex) {
                logger.error("Failed to send error message: {}", ex.getMessage(), ex);
            }
        }
    }
}
