package com.example.notification_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Value("${kafka.topic.file-processed}")
    private String fileProcessedTopic;

    @Value("${kafka.topic.file-error}")
    private String fileErrorTopic;

    @Value("${app.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${app.retry.delay:5000}")
    private long retryDelay;

    @KafkaListener(topics = "${kafka.topic.file-processed}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleFileProcessed(String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            logger.info("Received file processed notification from topic {}: {}", topic, message);
            
            // Parse the message
            Map<String, String> processedDetails = objectMapper.readValue(message, new com.fasterxml.jackson.core.type.TypeReference<HashMap<String, String>>() {});
            String fileId = processedDetails.get("fileId");
            String originalPath = processedDetails.get("originalPath");
            String processedPath = processedDetails.get("processedPath");
            String status = processedDetails.get("status");
            
            // Create notification message
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "SUCCESS");
            notification.put("fileId", fileId);
            notification.put("message", "File processing completed successfully!");
            notification.put("details", processedDetails);
            
            // Send WebSocket notification to all connected clients with retry
            sendWebSocketNotificationWithRetry("/topic/notifications", notification);
            logger.info("Sent success notification via WebSocket");
            
        } catch (Exception e) {
            logger.error("Error handling file processed notification: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "${kafka.topic.file-error}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleFileError(String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            logger.info("Received file error notification from topic {}: {}", topic, message);
            
            // Parse the message
            Map<String, String> errorDetails = objectMapper.readValue(message, new com.fasterxml.jackson.core.type.TypeReference<HashMap<String, String>>() {});
            String fileId = errorDetails.get("fileId");
            String error = errorDetails.get("error");
            String errorMessage = errorDetails.get("errorMessage");
            
            // Create notification message
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "ERROR");
            notification.put("fileId", fileId);
            notification.put("message", "File processing failed!");
            notification.put("details", errorDetails);
            
            // Send WebSocket notification to all connected clients with retry
            sendWebSocketNotificationWithRetry("/topic/notifications", notification);
            logger.info("Sent error notification via WebSocket");
            
        } catch (Exception e) {
            logger.error("Error handling file error notification: {}", e.getMessage(), e);
        }
    }

    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 5000)
    )
    private void sendWebSocketNotificationWithRetry(String destination, Map<String, Object> notification) {
        try {
            messagingTemplate.convertAndSend(destination, notification);
            logger.info("WebSocket notification sent successfully to {}", destination);
        } catch (Exception e) {
            logger.error("Error sending WebSocket notification, attempt will be retried: {}", e.getMessage());
            throw new RuntimeException("Failed to send WebSocket notification", e);
        }
    }
} 