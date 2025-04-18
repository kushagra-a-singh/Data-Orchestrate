package com.mpjmp.orchestrator.service;

import com.mpjmp.orchestrator.model.PendingSync;
import com.mpjmp.orchestrator.model.FileChangeEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OfflineQueueService {
    private final DeviceRegistryService deviceRegistry;
    private final MongoTemplate mongoTemplate;
    
    // --- REMOVED KAFKA DEPENDENCIES AND LISTENERS ---
    // --- UPDATED TO USE DIRECT QUEUE MANAGEMENT ---
    
    public void handleOfflineDevices(FileChangeEvent event) {
        deviceRegistry.getOfflineDevices().forEach(device -> {
            mongoTemplate.save(new PendingSync(
                device.getId(),
                event.getFileId(),
                event.getFilename(),
                System.currentTimeMillis()
            ));
        });
    }
}
