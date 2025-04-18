package com.mpjmp.orchestrator.service;

import com.mpjmp.orchestrator.model.PendingSync;
import com.mpjmp.orchestrator.model.DeviceInfo;
import com.mpjmp.orchestrator.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OfflineSyncService {
    
    private static final Logger log = LoggerFactory.getLogger(OfflineSyncService.class);

    private final MongoTemplate mongoTemplate;
    private final DeviceRepository deviceRepository;
    private final FileSyncService fileSyncService;
    private final RestTemplate restTemplate;
    
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void syncOfflineDevices() {
        List<DeviceInfo> offlineDevices = deviceRepository.findByOnlineFalse();
        offlineDevices.forEach(device -> {
            try {
                if (checkDeviceOnline(device)) {
                    device.setOnline(true);
                    deviceRepository.save(device);
                    
                    // Process pending syncs
                    mongoTemplate.find(
                        new Query(Criteria.where("deviceId").is(device.getId())), 
                        PendingSync.class
                    ).forEach(sync -> {
                        fileSyncService.replicateFileToDevices(sync.getFileId());
                        mongoTemplate.remove(sync);
                    });
                }
            } catch (Exception e) {
                log.error("Sync failed for device {}: {}", device.getId(), e.getMessage());
            }
        });
    }

    private boolean checkDeviceOnline(DeviceInfo device) {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                device.getHealthCheckUrl(), 
                String.class
            );
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }
}
