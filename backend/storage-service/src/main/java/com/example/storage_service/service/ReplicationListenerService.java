package com.example.storage_service.service;

import com.example.storage_service.model.DeviceInfo;
import com.example.storage_service.model.ReplicationRequest;
import com.example.storage_service.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ReplicationListenerService {

    private final StorageService storageService;
    private final DeviceRepository deviceRepository;

    // --- REMOVE KAFKA CONSUMER LOGIC, USE HTTP INSTEAD ---
    // Add HTTP endpoint for file replication
    // Old KafkaListener logic removed as part of Kafka dependency cleanup
    @PostMapping("/replicate-file")
    public ResponseEntity<String> receiveFileReplicationRequest(@RequestBody ReplicationRequest request) {
        log.info("Received file replication request for fileId: {} from sourceDeviceUrl: {}", request.getFileId(), request.getSourceDeviceUrl());
        try {
            // Build the download URL using the correct endpoint as tested in Postman
            String downloadUrl = request.getSourceDeviceUrl() + "/api/files/download/" + request.getFileId();
            log.info("Attempting to download file from: {}", downloadUrl);

            // Use RestTemplate to download the file as byte[]
            org.springframework.http.ResponseEntity<byte[]> response = new org.springframework.web.client.RestTemplate().getForEntity(downloadUrl, byte[].class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // Save the file to the replicated directory (e.g., storageDir/replicated/filename)
                java.nio.file.Path replicatedDir = java.nio.file.Paths.get("./data/replicated");
                java.nio.file.Files.createDirectories(replicatedDir);
                java.nio.file.Path targetPath = replicatedDir.resolve(request.getFileName());
                java.nio.file.Files.write(targetPath, response.getBody());
                log.info("File {} replicated and saved to {}", request.getFileName(), targetPath.toAbsolutePath());
                return ResponseEntity.ok("File replicated and saved to: " + targetPath.toAbsolutePath());
            } else {
                log.error("Failed to download file from {}: status {}", downloadUrl, response.getStatusCode());
                return ResponseEntity.status(500).body("Failed to download file from source device.");
            }
        } catch (Exception e) {
            log.error("Exception during file replication: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Exception during file replication: " + e.getMessage());
        }
    }
}