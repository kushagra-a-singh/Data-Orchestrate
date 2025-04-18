// Deleted: This service was only used for Kafka event consumption and is obsolete after HTTP migration.

package com.mpjmp.orchestrator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mpjmp.orchestrator.model.FileEvent;
import com.mpjmp.orchestrator.repository.FileEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
    private final ObjectMapper objectMapper;

    @Value("${app.sync.dir}")
    private String syncDir;

    public void handleFileUploadEvent(Map<String, Object> event) {
        try {
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
            fileEventRepository.save(fileEvent);
            // Trigger replication via FileSyncService (direct call)
            // fileSyncService.replicateFileToDevices(fileEvent.getFileId());
        } catch (Exception e) {
            log.error("Error handling file upload event", e);
            throw new RuntimeException("Failed to handle file upload event", e);
        }
    }

    public void handleFileDeletionEvent(Map<String, Object> event) {
        try {
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
            fileEventRepository.save(fileEvent);
        } catch (Exception e) {
            log.error("Error handling file deletion event", e);
            throw new RuntimeException("Failed to handle file deletion event", e);
        }
    }

    public void handleFileStatusChangeEvent(Map<String, Object> event) {
        try {
            // Create and save file event
            FileEvent fileEvent = new FileEvent();
            fileEvent.setFileId((String) event.get("fileId"));
            fileEvent.setFileName((String) event.get("fileName"));
            fileEvent.setEventType("STATUS_CHANGE");
            fileEvent.setStatus((String) event.get("newStatus"));
            fileEvent.setErrorMessage((String) event.get("errorMessage"));
            fileEvent.setTimestamp(LocalDateTime.now());
            fileEvent.setMetadata(event);
            fileEventRepository.save(fileEvent);
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