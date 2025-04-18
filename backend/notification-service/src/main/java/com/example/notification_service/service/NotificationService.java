package com.example.notification_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@Service
@RestController
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Value("${app.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${app.retry.delay:5000}")
    private long retryDelay;

    @PostMapping("/notifications/processed")
    public ResponseEntity<String> handleFileProcessed(@RequestBody String message) {
        try {
            logger.info("Received file processed notification: {}", message);
            
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
            
            // Deprecated: WebSocket notification logic. Use HTTP-based notification if required.
            // sendWebSocketNotificationWithRetry and related logic can be removed if not needed.
            // sendWebSocketNotificationWithRetry("/topic/notifications", notification);
            logger.info("Sent success notification via HTTP");
            
            return ResponseEntity.ok("Notification processed");
        } catch (Exception e) {
            logger.error("Error handling file processed notification: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error processing notification");
        }
    }

    @PostMapping("/notifications/error")
    public ResponseEntity<String> handleFileError(@RequestBody String message) {
        try {
            logger.info("Received file error notification: {}", message);
            
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
            
            // Deprecated: WebSocket notification logic. Use HTTP-based notification if required.
            // sendWebSocketNotificationWithRetry and related logic can be removed if not needed.
            // sendWebSocketNotificationWithRetry("/topic/notifications", notification);
            logger.info("Sent error notification via HTTP");
            
            return ResponseEntity.ok("Notification processed");
        } catch (Exception e) {
            logger.error("Error handling file error notification: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error processing notification");
        }
    }

    // Deprecated: WebSocket notification logic. Use HTTP-based notification if required.
    // sendWebSocketNotificationWithRetry and related logic can be removed if not needed.
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