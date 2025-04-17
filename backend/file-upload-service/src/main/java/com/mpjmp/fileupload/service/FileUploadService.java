package com.mpjmp.fileupload.service;

import com.dataorchestrate.common.DeviceIdentifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mpjmp.fileupload.kafka.ReplicationEvent;
import com.mpjmp.fileupload.kafka.ReplicationProducer;
import com.mpjmp.fileupload.model.FileMetadata;
import com.mpjmp.fileupload.repository.DeviceRepository;
import com.mpjmp.fileupload.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final FileMetadataRepository fileMetadataRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final MongoTemplate mongoTemplate;
    @Autowired
    private ReplicationProducer replicationProducer;
    @Autowired
    private DeviceRepository deviceRepository;

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${kafka.topic.file-upload}")
    private String fileUploadTopic;

    @Value("${kafka.topic.notifications}")
    private String notificationsTopic;

    @Value("${kafka.topic.file-deleted}")
    private String fileDeletedTopic;

    @Value("${kafka.topic.file-status}")
    private String fileStatusTopic;

    @Value("${app.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${app.retry.delay:5000}")
    private long retryDelay;

    @Value("${server.port}")
    private int serverPort;

    public FileMetadata uploadFile(MultipartFile file, String uploadedBy, String deviceName, String deviceIp) throws IOException {
        DeviceIdentifier deviceIdentifier = new DeviceIdentifier();
        String dynamicDeviceId = deviceIdentifier.getDeviceId();
        String dynamicDeviceName = deviceIdentifier.getDeviceName(); // Use getDeviceName() instead of getDeviceId()
        String dynamicDeviceIp = deviceIp != null ? deviceIp : InetAddress.getLocalHost().getHostAddress();
        
        // Add null checks to prevent NullPointerException
        if (uploadDir == null || uploadDir.isEmpty()) {
            uploadDir = "uploads"; // Default directory if not configured
            log.warn("Upload directory not configured, using default: {}", uploadDir);
        }
        
        if (dynamicDeviceId == null || dynamicDeviceId.isEmpty()) {
            dynamicDeviceId = "unknown-device"; // Default device ID if not available
            log.warn("Device ID is null or empty, using default: {}", dynamicDeviceId);
        }
        
        Path uploadPath = Paths.get(uploadDir, dynamicDeviceId);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            originalFilename = "unnamed-file.bin";
            log.warn("Original filename is null or empty, using default: {}", originalFilename);
        }
        
        String fileExtension = originalFilename.contains(".") ? 
            originalFilename.substring(originalFilename.lastIndexOf(".")) : ".bin";
        String fileName = UUID.randomUUID().toString() + fileExtension;
        saveFileWithRetry(file, uploadPath, fileName);
        Path savedFilePath = uploadPath.resolve(fileName);
        if (Files.exists(savedFilePath)) {
            log.info("File saved successfully at: {}", savedFilePath.toAbsolutePath());
        } else {
            log.error("File was NOT saved at: {}", savedFilePath.toAbsolutePath());
        }
        FileMetadata metadata = new FileMetadata();
        metadata.setFileName(fileName);
        metadata.setOriginalFileName(originalFilename);
        metadata.setContentType(file.getContentType());
        metadata.setSize(file.getSize());
        metadata.setStatus("UPLOADED");
        metadata.setUploadedBy(uploadedBy);
        metadata.setDeviceName(dynamicDeviceName);
        metadata.setDeviceId(dynamicDeviceId);
        metadata.setDeviceIp(dynamicDeviceIp);
        metadata.setUploadedAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
        metadata.setStoragePath(savedFilePath.toAbsolutePath().toString());
        FileMetadata savedMetadata = fileMetadataRepository.save(metadata);
        try {
            sendFileUploadEventWithRetry(savedMetadata, originalFilename, file, uploadedBy);
            savedMetadata.setStatus("UPLOADED");
            savedMetadata = fileMetadataRepository.save(savedMetadata);
            sendNotificationWithRetry("SUCCESS", "File uploaded successfully: " + originalFilename);
            log.info("Notification sent for file upload: {}", originalFilename);

            // --- Replication Event Publishing ---
            List<String> allDeviceIds = deviceRepository.findAll().stream()
                .map(d -> d.getDeviceId())
                .filter(id -> !id.equals(metadata.getDeviceId()))
                .toList();
            String downloadUrl = "http://" + metadata.getDeviceIp() + ":" + serverPort + "/api/files/download/" + metadata.getDeviceId() + "/" + metadata.getFileName();
            ReplicationEvent replicationEvent = new ReplicationEvent();
            replicationEvent.setFileId(savedMetadata.getId());
            replicationEvent.setFileName(savedMetadata.getFileName());
            replicationEvent.setUploaderDeviceId(savedMetadata.getDeviceId());
            replicationEvent.setUploaderDeviceName(savedMetadata.getDeviceName());
            replicationEvent.setDownloadUrl(downloadUrl);
            replicationEvent.setTargetDeviceIds(allDeviceIds);
            replicationEvent.setStatus("INITIATED");
            replicationProducer.sendReplicationEvent("file-replication", replicationEvent);
            log.info("Replication event sent for file {} to devices {}", savedMetadata.getFileName(), allDeviceIds);
        } catch (Exception e) {
            savedMetadata.setStatus("FAILED");
            savedMetadata.setErrorMessage("Failed to process file: " + e.getMessage());
            savedMetadata = fileMetadataRepository.save(savedMetadata);
            sendNotificationWithRetry("ERROR", "Failed to process file: " + e.getMessage());
            log.error("Notification sent for FAILED file upload: {}", originalFilename);
            throw new RuntimeException("Failed to process file", e);
        }
        return savedMetadata;
    }

    @Retryable(
        value = {IOException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 5000)
    )
    private void saveFileWithRetry(MultipartFile file, Path uploadPath, String fileName) throws IOException {
        try {
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);
            log.info("File saved successfully: {}", fileName);
        } catch (IOException e) {
            log.error("Error saving file, attempt will be retried: {}", e.getMessage());
            throw e;
        }
    }

    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 5000)
    )
    private void sendFileUploadEventWithRetry(FileMetadata savedMetadata, String originalFilename, MultipartFile file, String uploadedBy) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("fileId", savedMetadata.getId());
            event.put("fileName", savedMetadata.getFileName());
            event.put("originalFileName", originalFilename);
            event.put("contentType", file.getContentType());
            event.put("size", file.getSize());
            event.put("uploadedBy", uploadedBy);
            event.put("uploadedAt", savedMetadata.getUploadedAt().toString());

            kafkaTemplate.send(fileUploadTopic, objectMapper.writeValueAsString(event));
            log.info("File upload event sent successfully for file: {}", originalFilename);
        } catch (Exception e) {
            log.error("Error sending file upload event, attempt will be retried: {}", e.getMessage());
            throw new RuntimeException("Failed to send file upload event", e);
        }
    }

    public FileMetadata getFileMetadata(String fileId) {
        return fileMetadataRepository.findById(fileId).orElse(null);
    }

    public List<FileMetadata> listFiles(String status, String uploadedBy) {
        Query query = new Query();
        
        if (status != null && !status.isEmpty()) {
            query.addCriteria(Criteria.where("status").is(status));
        }
        
        if (uploadedBy != null && !uploadedBy.isEmpty()) {
            query.addCriteria(Criteria.where("uploadedBy").is(uploadedBy));
        }
        
        return mongoTemplate.find(query, FileMetadata.class);
    }

    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 5000)
    )
    public boolean deleteFile(String fileId) {
        try {
            FileMetadata metadata = fileMetadataRepository.findById(fileId).orElse(null);
            if (metadata == null) {
                return false;
            }

            // Delete the physical file
            Path filePath = Paths.get(uploadDir, metadata.getDeviceId(), metadata.getFileName());
            Files.deleteIfExists(filePath);

            // Delete metadata from MongoDB
            fileMetadataRepository.deleteById(fileId);

            // Send notification with retry
            sendNotificationWithRetry("SUCCESS", "File deleted successfully: " + metadata.getOriginalFileName());

            // Notify orchestrator about file deletion with retry
            sendFileDeletionEventWithRetry(fileId, metadata);

            return true;
        } catch (Exception e) {
            log.error("Error deleting file: " + fileId, e);
            sendNotificationWithRetry("ERROR", "Failed to delete file: " + e.getMessage());
            throw new RuntimeException("Failed to delete file", e);
        }
    }

    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 5000)
    )
    private void sendFileDeletionEventWithRetry(String fileId, FileMetadata metadata) {
        try {
            Map<String, Object> deleteEvent = new HashMap<>();
            deleteEvent.put("fileId", fileId);
            deleteEvent.put("fileName", metadata.getFileName());
            deleteEvent.put("originalFileName", metadata.getOriginalFileName());
            deleteEvent.put("deletedAt", LocalDateTime.now().toString());
            deleteEvent.put("deletedBy", metadata.getUploadedBy());

            kafkaTemplate.send(fileDeletedTopic, objectMapper.writeValueAsString(deleteEvent));
            log.info("File deletion event sent successfully for file: {}", metadata.getOriginalFileName());
        } catch (Exception e) {
            log.error("Error sending file deletion event, attempt will be retried: {}", e.getMessage());
            throw new RuntimeException("Failed to send file deletion event", e);
        }
    }

    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 5000)
    )
    public void updateFileStatus(String fileId, String newStatus, String errorMessage) {
        try {
            FileMetadata metadata = fileMetadataRepository.findById(fileId).orElse(null);
            if (metadata != null) {
                String oldStatus = metadata.getStatus();
                metadata.setStatus(newStatus);
                metadata.setErrorMessage(errorMessage);
                if (newStatus.equals("COMPLETED")) {
                    metadata.setProcessedAt(LocalDateTime.now());
                }
                fileMetadataRepository.save(metadata);

                // Notify orchestrator about status change with retry
                sendFileStatusEventWithRetry(fileId, metadata, oldStatus, newStatus, errorMessage);

                // Send notification with retry
                String message = newStatus.equals("COMPLETED") 
                    ? "File processing completed: " + metadata.getOriginalFileName()
                    : "File processing failed: " + metadata.getOriginalFileName();
                sendNotificationWithRetry(newStatus.equals("COMPLETED") ? "SUCCESS" : "ERROR", message);
            }
        } catch (Exception e) {
            log.error("Error updating file status: " + fileId, e);
            sendNotificationWithRetry("ERROR", "Failed to update file status: " + e.getMessage());
            throw new RuntimeException("Failed to update file status", e);
        }
    }

    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 5000)
    )
    private void sendFileStatusEventWithRetry(String fileId, FileMetadata metadata, String oldStatus, String newStatus, String errorMessage) {
        try {
            Map<String, Object> statusEvent = new HashMap<>();
            statusEvent.put("fileId", fileId);
            statusEvent.put("fileName", metadata.getFileName());
            statusEvent.put("oldStatus", oldStatus);
            statusEvent.put("newStatus", newStatus);
            statusEvent.put("errorMessage", errorMessage);
            statusEvent.put("updatedAt", LocalDateTime.now().toString());

            kafkaTemplate.send(fileStatusTopic, objectMapper.writeValueAsString(statusEvent));
            log.info("File status event sent successfully for file: {}", metadata.getOriginalFileName());
        } catch (Exception e) {
            log.error("Error sending file status event, attempt will be retried: {}", e.getMessage());
            throw new RuntimeException("Failed to send file status event", e);
        }
    }

    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 5000)
    )
    private void sendNotificationWithRetry(String type, String message) {
        try {
            Map<String, String> notification = new HashMap<>();
            notification.put("type", type);
            notification.put("message", message);
            kafkaTemplate.send(notificationsTopic, objectMapper.writeValueAsString(notification));
            log.info("Notification sent successfully: {}", message);
        } catch (Exception e) {
            log.error("Failed to send notification, attempt will be retried: {}", e.getMessage());
            throw new RuntimeException("Failed to send notification", e);
        }
    }

    public FileMetadata updateFileMetadata(FileMetadata metadata) {
        return fileMetadataRepository.save(metadata);
    }
}