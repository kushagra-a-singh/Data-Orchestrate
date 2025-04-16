package com.mpjmp.orchestrator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileSyncService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${app.sync.dir}")
    private String syncDir;

    @Value("${kafka.topic.file-sync}")
    private String fileSyncTopic;

    @Value("${app.device.id}")
    private String deviceId;

    // Track file versions to handle conflicts
    private final ConcurrentHashMap<String, Long> fileVersions = new ConcurrentHashMap<>();

    @KafkaListener(topics = "${kafka.topic.file-upload}", groupId = "sync-group")
    public void handleFileUpload(String message) {
        try {
            Map<String, Object> event = objectMapper.readValue(message, new com.fasterxml.jackson.core.type.TypeReference<HashMap<String, Object>>() {});
            String fileId = (String) event.get("fileId");
            String fileName = (String) event.get("fileName");
            String sourceDeviceId = (String) event.get("deviceId");
            
            // Skip if this is our own upload
            if (deviceId.equals(sourceDeviceId)) {
                return;
            }

            // Download file from source device
            downloadFile(fileId, fileName, sourceDeviceId);
            
            // Update local version
            fileVersions.put(fileId, System.currentTimeMillis());
            
            log.info("File synchronized: {} from device: {}", fileName, sourceDeviceId);
        } catch (Exception e) {
            log.error("Error handling file upload for sync", e);
        }
    }

    @KafkaListener(topics = "file.processed", groupId = "sync-group")
    public void handleFileProcessed(String message) {
        try {
            Map<String, Object> event = objectMapper.readValue(message, new com.fasterxml.jackson.core.type.TypeReference<HashMap<String, Object>>() {});
            String fileId = (String) event.get("fileId");
            String fileName = (String) event.get("fileName");
            String sourceDeviceId = (String) event.get("deviceId");
            if (deviceId.equals(sourceDeviceId)) {
                return;
            }
            downloadFile(fileId, fileName, sourceDeviceId);
            fileVersions.put(fileId, System.currentTimeMillis());
            log.info("File processed and synchronized: {} from device: {}", fileName, sourceDeviceId);
        } catch (Exception e) {
            log.error("Error handling file processed for sync", e);
        }
    }

    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 5000)
    )
    private void downloadFile(String fileId, String fileName, String sourceDeviceId) throws IOException {
        // Create sync directory if it doesn't exist
        Path syncPath = Paths.get(syncDir);
        if (!Files.exists(syncPath)) {
            Files.createDirectories(syncPath);
        }

        // Download file from source device
        String downloadUrl = String.format("http://%s:8081/api/files/%s/download", sourceDeviceId, fileId);
        byte[] fileContent = restTemplate.getForObject(downloadUrl, byte[].class);

        // Save file
        Path filePath = syncPath.resolve(fileName);
        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            fos.write(fileContent);
        }
    }

    public void handleFileConflict(String fileId, String fileName, String sourceDeviceId, long sourceVersion) {
        long localVersion = fileVersions.getOrDefault(fileId, 0L);
        
        if (sourceVersion > localVersion) {
            // Remote version is newer, download it
            try {
                downloadFile(fileId, fileName, sourceDeviceId);
                fileVersions.put(fileId, sourceVersion);
                log.info("Resolved conflict for file {}: using version from device {}", fileName, sourceDeviceId);
            } catch (Exception e) {
                log.error("Error resolving file conflict", e);
            }
        } else if (sourceVersion < localVersion) {
            // Local version is newer, send it to the source device
            try {
                uploadFile(fileId, fileName, sourceDeviceId);
                log.info("Resolved conflict for file {}: sent local version to device {}", fileName, sourceDeviceId);
            } catch (Exception e) {
                log.error("Error sending local version to resolve conflict", e);
            }
        } else {
            // Versions are the same, no action needed
            log.info("No conflict resolution needed for file {}: versions are identical", fileName);
        }
    }

    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 5000)
    )
    private void uploadFile(String fileId, String fileName, String targetDeviceId) throws IOException {
        Path filePath = Paths.get(syncDir, fileName);
        byte[] fileContent = Files.readAllBytes(filePath);

        // Send file to target device
        String uploadUrl = String.format("http://%s:8081/api/files/sync", targetDeviceId);
        Map<String, Object> request = new HashMap<>();
        request.put("fileId", fileId);
        request.put("fileName", fileName);
        request.put("content", fileContent);
        request.put("version", fileVersions.get(fileId));

        restTemplate.postForObject(uploadUrl, request, Void.class);
    }
} 