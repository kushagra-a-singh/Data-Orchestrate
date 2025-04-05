package com.example.storage_service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.DirectoryStream;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;
import java.util.Set;

@Service
public class StorageService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final FileDistributionService distributionService;
    private final String deviceId;

    @Value("${file.storage-dir}")
    private String storageDir;

    @Autowired
    public StorageService(
            KafkaTemplate<String, String> kafkaTemplate,
            FileDistributionService distributionService,
            @Value("${device.id}") String deviceId) {
        this.kafkaTemplate = kafkaTemplate;
        this.distributionService = distributionService;
        this.deviceId = deviceId;
    }

    @PostConstruct
    public void init() {
        // Ensure storage directory exists
        try {
            Files.createDirectories(Paths.get(storageDir));
        } catch (IOException e) {
            throw new RuntimeException("Could not create storage directory", e);
        }
    }

    public void storeFile(byte[] fileData, String fileName) throws IOException {
        // Clean up old files first
        cleanupOldFiles();

        // Generate a unique file ID
        String fileId = java.util.UUID.randomUUID().toString();
        String filePath = Paths.get(storageDir, fileId).toString();
        
        // Store the file locally
        Files.write(Paths.get(filePath), fileData);

        // Register the file for P2P distribution
        distributionService.registerNewFile(fileId, fileName, deviceId, filePath, fileData.length);

        // Notify other services about the new file
        kafkaTemplate.send("file-replication-topic", fileId + "," + deviceId);
    }

    private void cleanupOldFiles() {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(storageDir))) {
            for (Path entry : stream) {
                BasicFileAttributes attrs = Files.readAttributes(entry, BasicFileAttributes.class);
                long fileAgeInDays = TimeUnit.MILLISECONDS.toDays(
                    System.currentTimeMillis() - attrs.creationTime().toMillis()
                );
                if (fileAgeInDays > 30) { // Delete files older than 30 days
                    Files.delete(entry);
                }
            }
        } catch (IOException e) {
            // Log error but don't throw - this is a maintenance operation
            System.err.println("Error cleaning up old files: " + e.getMessage());
        }
    }

    public byte[] getFile(String fileId) throws IOException {
        Path filePath = Paths.get(storageDir, fileId);
        if (Files.exists(filePath)) {
            return Files.readAllBytes(filePath);
        }
        
        // If we don't have the file locally, try to get it from another device
        Set<String> availableDevices = distributionService.getAvailableDevicesForFile(fileId);
        if (!availableDevices.isEmpty()) {
            try {
                // File exists in the network but not locally - trigger replication
                distributionService.handleDeviceOnline(deviceId);
                // Wait briefly for replication
                Thread.sleep(1000);
                // Try reading again
                if (Files.exists(filePath)) {
                    return Files.readAllBytes(filePath);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("File replication was interrupted", e);
            }
        }
        
        throw new IOException("File not found: " + fileId);
    }
}
