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
import java.nio.file.StandardCopyOption;
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
    
    @Value("${app.data.dir:data}")
    private String dataDir;

    @Value("${kafka.topic.file-sync}")
    private String fileSyncTopic;

    @Value("${app.device.id}")
    private String deviceId;

    // Track file versions to handle conflicts
    private final ConcurrentHashMap<String, Long> fileVersions = new ConcurrentHashMap<>();
    
    /**
     * Initialize directories on startup
     */
    @javax.annotation.PostConstruct
    public void init() {
        try {
            // Create sync directory if it doesn't exist
            Path syncPath = Paths.get(syncDir);
            if (!Files.exists(syncPath)) {
                Files.createDirectories(syncPath);
                log.info("Created sync directory: {}", syncPath);
            }
            
            // Create data directory if it doesn't exist
            Path dataPath = Paths.get(dataDir);
            if (!Files.exists(dataPath)) {
                Files.createDirectories(dataPath);
                log.info("Created data directory: {}", dataPath);
            }
            
            // Create device-specific data directory
            Path deviceDataPath = Paths.get(dataDir, deviceId);
            if (!Files.exists(deviceDataPath)) {
                Files.createDirectories(deviceDataPath);
                log.info("Created device data directory: {}", deviceDataPath);
            }
        } catch (Exception e) {
            log.error("Error initializing directories", e);
        }
    }

    @KafkaListener(topics = "${kafka.topic.file-upload}", groupId = "sync-group")
    public void handleFileUpload(String message) {
        try {
            Map<String, Object> event = objectMapper.readValue(message, new com.fasterxml.jackson.core.type.TypeReference<HashMap<String, Object>>() {});
            String fileId = (String) event.get("fileId");
            String fileName = (String) event.get("fileName");
            String sourceDeviceId = (String) event.get("deviceId");
            
            log.info("Received file upload event: {} from device: {}", fileName, sourceDeviceId);
            
            // Skip if this is our own upload
            if (deviceId.equals(sourceDeviceId)) {
                log.debug("Skipping our own upload event for file: {}", fileName);
                return;
            }

            // Download file from source device
            downloadFile(fileId, fileName, sourceDeviceId);
            
            // Update local version
            fileVersions.put(fileId, System.currentTimeMillis());
            
            // Copy the file to the data directory
            Path syncFilePath = Paths.get(syncDir, fileName);
            Path dataFilePath = Paths.get(dataDir, deviceId, fileName);
            
            if (Files.exists(syncFilePath)) {
                Files.copy(syncFilePath, dataFilePath, StandardCopyOption.REPLACE_EXISTING);
                log.info("Copied synchronized file to data directory: {}", dataFilePath);
                
                // Send notification about successful synchronization
                sendSyncNotification(fileId, fileName, sourceDeviceId, "SUCCESS", 
                    "File synchronized successfully: " + fileName);
            } else {
                log.error("Synchronized file not found in sync directory: {}", syncFilePath);
                sendSyncNotification(fileId, fileName, sourceDeviceId, "ERROR", 
                    "File synchronization failed: " + fileName);
            }
            
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
            
            log.info("Received file processed event: {} from device: {}", fileName, sourceDeviceId);
            
            if (deviceId.equals(sourceDeviceId)) {
                log.debug("Skipping our own processed event for file: {}", fileName);
                return;
            }
            
            downloadFile(fileId, fileName, sourceDeviceId);
            fileVersions.put(fileId, System.currentTimeMillis());
            
            // Copy the file to the data directory
            Path syncFilePath = Paths.get(syncDir, fileName);
            Path dataFilePath = Paths.get(dataDir, deviceId, fileName);
            
            if (Files.exists(syncFilePath)) {
                Files.copy(syncFilePath, dataFilePath, StandardCopyOption.REPLACE_EXISTING);
                log.info("Copied processed file to data directory: {}", dataFilePath);
                
                // Also download and store extracted text if available
                try {
                    String textDownloadUrl = String.format("http://%s:8081/api/files/%s/text", sourceDeviceId, fileId);
                    String extractedText = restTemplate.getForObject(textDownloadUrl, String.class);
                    
                    if (extractedText != null && !extractedText.isEmpty()) {
                        Path textFilePath = Paths.get(dataDir, deviceId, fileName + ".txt");
                        Files.write(textFilePath, extractedText.getBytes());
                        log.info("Saved extracted text to: {}", textFilePath);
                    }
                } catch (Exception e) {
                    log.warn("Could not download extracted text for file: {}", fileName, e);
                }
                
                sendSyncNotification(fileId, fileName, sourceDeviceId, "SUCCESS", 
                    "Processed file synchronized successfully: " + fileName);
            } else {
                log.error("Processed file not found in sync directory: {}", syncFilePath);
                sendSyncNotification(fileId, fileName, sourceDeviceId, "ERROR", 
                    "Processed file synchronization failed: " + fileName);
            }
            
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

        log.info("Downloading file {} from device {}", fileName, sourceDeviceId);
        
        // Download file from source device
        String downloadUrl = String.format("http://%s:8081/api/files/%s/download", sourceDeviceId, fileId);
        byte[] fileContent = restTemplate.getForObject(downloadUrl, byte[].class);

        if (fileContent == null || fileContent.length == 0) {
            throw new IOException("Downloaded file content is empty for file: " + fileName);
        }
        
        // Save file to sync directory
        Path filePath = syncPath.resolve(fileName);
        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            fos.write(fileContent);
        }
        
        log.info("Downloaded file saved to: {}, size: {} bytes", filePath, fileContent.length);
    }

    public void handleFileConflict(String fileId, String fileName, String sourceDeviceId, long sourceVersion) {
        long localVersion = fileVersions.getOrDefault(fileId, 0L);
        
        log.info("Handling file conflict for {}: local version {}, remote version {}", 
            fileName, localVersion, sourceVersion);
        
        if (sourceVersion > localVersion) {
            // Remote version is newer, download it
            try {
                downloadFile(fileId, fileName, sourceDeviceId);
                fileVersions.put(fileId, sourceVersion);
                
                // Copy to data directory
                Path syncFilePath = Paths.get(syncDir, fileName);
                Path dataFilePath = Paths.get(dataDir, deviceId, fileName);
                Files.copy(syncFilePath, dataFilePath, StandardCopyOption.REPLACE_EXISTING);
                
                log.info("Resolved conflict for file {}: using version from device {}", fileName, sourceDeviceId);
                
                sendSyncNotification(fileId, fileName, sourceDeviceId, "INFO", 
                    "File conflict resolved: using newer version from device " + sourceDeviceId);
            } catch (Exception e) {
                log.error("Error resolving file conflict", e);
            }
        } else if (sourceVersion < localVersion) {
            // Local version is newer, send it to the source device
            try {
                uploadFile(fileId, fileName, sourceDeviceId);
                log.info("Resolved conflict for file {}: sent local version to device {}", fileName, sourceDeviceId);
                
                sendSyncNotification(fileId, fileName, sourceDeviceId, "INFO", 
                    "File conflict resolved: sent newer local version to device " + sourceDeviceId);
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
        // First check data directory
        Path dataFilePath = Paths.get(dataDir, deviceId, fileName);
        Path filePath;
        
        if (Files.exists(dataFilePath)) {
            filePath = dataFilePath;
        } else {
            // Fallback to sync directory
            filePath = Paths.get(syncDir, fileName);
            if (!Files.exists(filePath)) {
                throw new IOException("File not found in either data or sync directory: " + fileName);
            }
        }
        
        log.info("Uploading file {} to device {}", fileName, targetDeviceId);
        
        byte[] fileContent = Files.readAllBytes(filePath);

        // Send file to target device
        String uploadUrl = String.format("http://%s:8081/api/files/sync", targetDeviceId);
        Map<String, Object> request = new HashMap<>();
        request.put("fileId", fileId);
        request.put("fileName", fileName);
        request.put("content", fileContent);
        request.put("version", fileVersions.getOrDefault(fileId, System.currentTimeMillis()));
        request.put("sourceDeviceId", deviceId);

        restTemplate.postForObject(uploadUrl, request, Void.class);
        log.info("File {} uploaded to device {}, size: {} bytes", fileName, targetDeviceId, fileContent.length);
    }
    
    private void sendSyncNotification(String fileId, String fileName, String sourceDeviceId, 
                                     String type, String message) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", type);
            notification.put("message", message);
            notification.put("fileId", fileId);
            notification.put("fileName", fileName);
            notification.put("sourceDeviceId", sourceDeviceId);
            notification.put("targetDeviceId", deviceId);
            notification.put("timestamp", LocalDateTime.now().toString());
            
            String notificationJson = objectMapper.writeValueAsString(notification);
            kafkaTemplate.send("notifications", notificationJson);
            
            // Also send replication status update
            Map<String, Object> statusUpdate = new HashMap<>();
            statusUpdate.put("fileId", fileId);
            statusUpdate.put("fileName", fileName);
            statusUpdate.put("sourceDeviceId", sourceDeviceId);
            statusUpdate.put("targetDeviceId", deviceId);
            statusUpdate.put("status", type.equals("ERROR") ? "FAILED" : "COMPLETED");
            statusUpdate.put("timestamp", LocalDateTime.now().toString());
            
            String statusJson = objectMapper.writeValueAsString(statusUpdate);
            kafkaTemplate.send("replication-status", statusJson);
            
            log.debug("Sent sync notification and status update for file: {}", fileName);
        } catch (Exception e) {
            log.error("Error sending sync notification", e);
        }
    }
}