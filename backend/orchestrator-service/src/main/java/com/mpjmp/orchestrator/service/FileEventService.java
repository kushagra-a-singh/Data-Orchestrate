package com.mpjmp.orchestrator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mpjmp.orchestrator.model.FileEvent;
import com.mpjmp.orchestrator.model.FileEvent;
import com.mpjmp.orchestrator.repository.FileEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FileEventService {

    private static final Logger log = LoggerFactory.getLogger(FileEventService.class);

    private final FileEventRepository fileEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topic.file-upload}")
    private String fileUploadTopic;

    @Value("${kafka.topic.file-deleted}")
    private String fileDeletedTopic;

    @Value("${kafka.topic.file-status}")
    private String fileStatusTopic;

    @Value("${kafka.topic.processing-request}")
    private String processingRequestTopic;

    @Value("${app.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${app.retry.delay:5000}")
    private long retryDelay;

    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 5000)
    )
    public void handleFileUpload(String message) {
        try {
            Map<String, Object> event = parseEventMessage(message);
            
            // Create and save file event
            FileEvent fileEvent = new FileEvent();
            fileEvent.setFileId((String) event.get("fileId"));
            fileEvent.setFileName((String) event.get("fileName"));
            fileEvent.setOriginalFileName((String) event.get("originalFileName"));
            fileEvent.setEventType("UPLOAD");
            fileEvent.setStatus("UPLOADED");
            fileEvent.setUploadedBy((String) event.get("uploadedBy"));
            fileEvent.setTimestamp(LocalDateTime.now());
            fileEvent.setMetadata(event);
            
            saveFileEventWithRetry(fileEvent);
            sendProcessingRequestWithRetry(event);
            
            log.info("File upload event processed and processing requested for file: {}", fileEvent.getFileName());
        } catch (Exception e) {
            log.error("Error handling file upload event", e);
            throw new RuntimeException("Failed to handle file upload event", e);
        }
    }

    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 5000)
    )
    public void handleFileDeletion(String message) {
        try {
            Map<String, Object> event = parseEventMessage(message);
            
            // Create and save file event
            FileEvent fileEvent = new FileEvent();
            fileEvent.setFileId((String) event.get("fileId"));
            fileEvent.setFileName((String) event.get("fileName"));
            fileEvent.setOriginalFileName((String) event.get("originalFileName"));
            fileEvent.setEventType("DELETE");
            fileEvent.setStatus("DELETED");
            fileEvent.setUploadedBy((String) event.get("deletedBy"));
            fileEvent.setTimestamp(LocalDateTime.now());
            fileEvent.setMetadata(event);
            
            saveFileEventWithRetry(fileEvent);
            
            log.info("File deletion event processed for file: {}", fileEvent.getFileName());
        } catch (Exception e) {
            log.error("Error handling file deletion event", e);
            throw new RuntimeException("Failed to handle file deletion event", e);
        }
    }

    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 5000)
    )
    public void handleFileStatusChange(String message) {
        try {
            Map<String, Object> event = parseEventMessage(message);
            
            // Create and save file event
            FileEvent fileEvent = new FileEvent();
            fileEvent.setFileId((String) event.get("fileId"));
            fileEvent.setFileName((String) event.get("fileName"));
            fileEvent.setEventType("STATUS_CHANGE");
            fileEvent.setStatus((String) event.get("newStatus"));
            fileEvent.setErrorMessage((String) event.get("errorMessage"));
            fileEvent.setTimestamp(LocalDateTime.now());
            fileEvent.setMetadata(event);
            
            saveFileEventWithRetry(fileEvent);
            
            log.info("File status change event processed for file: {} - New status: {}", 
                    fileEvent.getFileName(), fileEvent.getStatus());
        } catch (Exception e) {
            log.error("Error handling file status change event", e);
            throw new RuntimeException("Failed to handle file status change event", e);
        }
    }

    private Map<String, Object> parseEventMessage(String message) throws JsonProcessingException {
        try {
            return objectMapper.readValue(message, new com.fasterxml.jackson.core.type.TypeReference<HashMap<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.error("Error parsing event message: {}", message, e);
            throw e;
        }
    }

    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 5000)
    )
    private void saveFileEventWithRetry(FileEvent fileEvent) {
        try {
            fileEventRepository.save(fileEvent);
            log.debug("File event saved successfully: {}", fileEvent.getFileId());
        } catch (Exception e) {
            log.error("Error saving file event: {}", fileEvent.getFileId(), e);
            throw new RuntimeException("Failed to save file event", e);
        }
    }

    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 5000)
    )
    private void sendProcessingRequestWithRetry(Map<String, Object> event) {
        try {
            Map<String, Object> processingRequest = new HashMap<>(event);
            processingRequest.put("requestType", "PROCESS");
            processingRequest.put("requestedAt", LocalDateTime.now().toString());
            
            kafkaTemplate.send(processingRequestTopic, objectMapper.writeValueAsString(processingRequest))
                .get(); // Wait for the send operation to complete
            
            log.debug("Processing request sent successfully for file: {}", event.get("fileId"));
        } catch (Exception e) {
            log.error("Error sending processing request for file: {}", event.get("fileId"), e);
            throw new RuntimeException("Failed to send processing request", e);
        }
    }

    public List<FileEvent> getFileHistory(String fileId) {
        return fileEventRepository.findByFileId(fileId);
    }

    public List<FileEvent> getEventsByType(String eventType) {
        return fileEventRepository.findByEventType(eventType);
    }

    public List<FileEvent> getEventsByStatus(String status) {
        return fileEventRepository.findByStatus(status);
    }
} 