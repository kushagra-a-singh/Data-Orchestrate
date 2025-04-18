package com.example.storage_service.service;

import com.example.storage_service.model.DeviceInfo;
import com.example.storage_service.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReplicationListenerService {

    private final StorageService storageService;
    private final DeviceRepository deviceRepository;

    // --- REMOVE HTTP REPLICATION, USE ONLY GRIDFS ---
    // Listen for file replication events and trigger background sync for the specific file
    @KafkaListener(topics = "${kafka.topic.file-replication}", groupId = "storage-group")
    public void handleFileReplication(String fileId) {
        log.info("Received file replication event for fileId: {}", fileId);
        List<DeviceInfo> devices = deviceRepository.findAll();
        devices.forEach(device -> {
            if (!device.isOffline()) {
                storageService.syncFileToDevice(fileId, device.getId());
            }
        });
    }

    // Listen for device online events and trigger full sync for that device
    @KafkaListener(topics = "${kafka.topic.device-online}", groupId = "storage-group")
    public void handleDeviceOnline(String deviceId) {
        log.info("Device {} is online, triggering full sync", deviceId);
        storageService.syncAllFilesToDevice(deviceId);
    }
}