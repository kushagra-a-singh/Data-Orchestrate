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
        log.info("Received file replication request for fileId: {}", request.getFileId());
        List<DeviceInfo> devices = deviceRepository.findAll();
        devices.forEach(device -> {
            if (!device.isOffline()) {
                try {
                    storageService.syncFileToDevice(request.getFileId(), device.getId());
                    log.info("Synced file {} to device {}", request.getFileId(), device.getId());
                } catch (Exception e) {
                    log.error("Failed to sync file {} to device {}: {}", request.getFileId(), device.getId(), e.getMessage());
                }
            }
        });
        return ResponseEntity.ok("File replicated successfully");
    }
}