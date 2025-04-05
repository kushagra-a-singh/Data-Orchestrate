package com.example.storage_service;

import com.example.storage_service.model.FileDistribution;
import com.example.storage_service.model.DeviceInfo;
import com.example.storage_service.model.LocalFileRegistry;
import com.example.storage_service.repository.FileDistributionRepository;
import com.example.storage_service.repository.DeviceRepository;
import com.example.storage_service.repository.LocalFileRegistryRepository;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;
import java.io.*;
import java.nio.file.*;

@Service
public class FileDistributionService {
    private static final Logger logger = LoggerFactory.getLogger(FileDistributionService.class);
    private final FileDistributionRepository distributionRepository;
    private final DeviceRepository deviceRepository;
    private final LocalFileRegistryRepository localFileRegistry;
    private final RestTemplate restTemplate;
    private static final int MAX_RETRY_ATTEMPTS = 10;
    private static final long[] RETRY_DELAYS = {5000, 10000, 30000, 60000, 300000}; // 5s, 10s, 30s, 1min, 5min

    @Autowired
    public FileDistributionService(
            FileDistributionRepository distributionRepository,
            DeviceRepository deviceRepository,
            LocalFileRegistryRepository localFileRegistry) {
        this.distributionRepository = distributionRepository;
        this.deviceRepository = deviceRepository;
        this.localFileRegistry = localFileRegistry;
        this.restTemplate = new RestTemplate();
    }

    public void registerNewFile(String fileId, String fileName, String uploaderDeviceId, String filePath, long fileSize) {
        // Register in distribution system
        FileDistribution distribution = new FileDistribution(fileId, fileName, uploaderDeviceId);
        distribution.addAvailableDevice(uploaderDeviceId);
        
        // Register locally for the uploader
        LocalFileRegistry localFile = new LocalFileRegistry(
            uploaderDeviceId, fileId, fileName, filePath, fileSize
        );
        localFileRegistry.save(localFile);
        
        // Add all other devices as pending
        List<DeviceInfo> allDevices = deviceRepository.findAll();
        for (DeviceInfo device : allDevices) {
            if (!device.getId().equals(uploaderDeviceId)) {
                distribution.addPendingDevice(device.getId());
            }
        }
        
        distributionRepository.save(distribution);
        logger.info("Registered new file {} for distribution", fileId);
    }

    @Scheduled(fixedDelay = 30000) // Run every 30 seconds
    public void processPendingReplications() {
        List<FileDistribution> pendingDistributions = distributionRepository.findAllWithPendingReplications();
        
        for (FileDistribution distribution : pendingDistributions) {
            processSingleDistribution(distribution);
        }
    }

    private void processSingleDistribution(FileDistribution distribution) {
        Set<String> pendingDevices = new HashSet<>(distribution.getPendingDevices());
        
        for (String pendingDeviceId : pendingDevices) {
            // Check if device already has the file locally
            Optional<LocalFileRegistry> existingFile = localFileRegistry
                .findByDeviceIdAndFileId(pendingDeviceId, distribution.getFileId());
            
            if (existingFile.isPresent() && existingFile.get().isVerified()) {
                // Device already has the file, update distribution status
                distribution.addAvailableDevice(pendingDeviceId);
                continue;
            }

            // Find best source device
            Optional<String> sourceDeviceId = findBestSourceDevice(distribution);
            if (sourceDeviceId.isPresent()) {
                tryReplicateFile(distribution, sourceDeviceId.get(), pendingDeviceId);
            } else {
                // No source available, will retry next cycle
                logger.info("No active source found for file {}, will retry later", distribution.getFileId());
            }
        }
        
        distributionRepository.save(distribution);
    }

    private Optional<String> findBestSourceDevice(FileDistribution distribution) {
        // Get all devices that have the file verified locally
        List<LocalFileRegistry> availableSources = localFileRegistry
            .findDevicesWithVerifiedFile(distribution.getFileId());
        
        // Filter to find active devices first
        Optional<LocalFileRegistry> activeSource = availableSources.stream()
            .filter(source -> {
                DeviceInfo device = deviceRepository.findById(source.getDeviceId()).orElse(null);
                return device != null && isDeviceActive(device);
            })
            .findFirst();
        
        if (activeSource.isPresent()) {
            return Optional.of(activeSource.get().getDeviceId());
        }
        
        // If no active devices, try offline devices (they might come online)
        return availableSources.stream()
            .map(LocalFileRegistry::getDeviceId)
            .findFirst();
    }

    private void tryReplicateFile(FileDistribution distribution, String sourceDeviceId, String targetDeviceId) {
        DeviceInfo sourceDevice = deviceRepository.findById(sourceDeviceId).orElse(null);
        DeviceInfo targetDevice = deviceRepository.findById(targetDeviceId).orElse(null);
        
        if (sourceDevice == null || targetDevice == null) {
            logger.error("Source or target device not found");
            return;
        }

        try {
            // Get source file details
            LocalFileRegistry sourceFile = localFileRegistry
                .findByDeviceIdAndFileId(sourceDeviceId, distribution.getFileId())
                .orElseThrow(() -> new IOException("Source file not found"));

            // Calculate retry delay based on attempts
            int attempts = getReplicationAttempts(targetDeviceId, distribution.getFileId());
            long delayMs = calculateRetryDelay(attempts);
            
            if (attempts > 0) {
                Thread.sleep(delayMs);
            }

            // Attempt replication
            String sourceUrl = "http://" + sourceDevice.getIpAddress() + "/download/" + distribution.getFileId();
            String targetUrl = "http://" + targetDevice.getIpAddress() + "/replicate";
            
            byte[] fileData = restTemplate.getForObject(sourceUrl, byte[].class);
            if (fileData != null) {
                restTemplate.postForObject(
                    targetUrl + "?fileName=" + distribution.getFileName(),
                    fileData,
                    String.class
                );
                
                // Register successful replication
                LocalFileRegistry targetFileRegistry = new LocalFileRegistry(
                    targetDeviceId,
                    distribution.getFileId(),
                    distribution.getFileName(),
                    sourceFile.getFilePath(),
                    sourceFile.getFileSize()
                );
                localFileRegistry.save(targetFileRegistry);
                
                // Update distribution status
                distribution.addAvailableDevice(targetDeviceId);
                
                logger.info("Successfully replicated file {} from {} to {}", 
                    distribution.getFileId(), sourceDeviceId, targetDeviceId);
            }
        } catch (Exception e) {
            logger.error("Failed to replicate file {} to device {}: {}", 
                distribution.getFileId(), targetDeviceId, e.getMessage());
            
            incrementReplicationAttempts(targetDeviceId, distribution.getFileId());
            
            // If max attempts reached, log but keep in pending for manual review
            if (getReplicationAttempts(targetDeviceId, distribution.getFileId()) >= MAX_RETRY_ATTEMPTS) {
                logger.error("Max retry attempts reached for file {} to device {}", 
                    distribution.getFileId(), targetDeviceId);
            }
        }
    }

    private long calculateRetryDelay(int attempts) {
        if (attempts <= 0) return 0;
        int index = Math.min(attempts - 1, RETRY_DELAYS.length - 1);
        return RETRY_DELAYS[index];
    }

    private int getReplicationAttempts(String deviceId, String fileId) {
        Optional<LocalFileRegistry> registry = localFileRegistry.findByDeviceIdAndFileId(deviceId, fileId);
        return registry.map(LocalFileRegistry::getVerificationAttempts).orElse(0);
    }

    private void incrementReplicationAttempts(String deviceId, String fileId) {
        localFileRegistry.findByDeviceIdAndFileId(deviceId, fileId).ifPresent(registry -> {
            registry.incrementVerificationAttempts();
            localFileRegistry.save(registry);
        });
    }

    public void handleDeviceOnline(String deviceId) {
        // Find all files that this device should have
        List<FileDistribution> pendingFiles = distributionRepository.findByPendingDevice(deviceId);
        
        for (FileDistribution distribution : pendingFiles) {
            processSingleDistribution(distribution);
        }
    }

    public void handleDeviceOffline(String deviceId) {
        DeviceInfo device = deviceRepository.findById(deviceId).orElse(null);
        if (device != null) {
            device.setLastActive(0);
            deviceRepository.save(device);
        }
    }

    private boolean isDeviceActive(DeviceInfo device) {
        long fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000);
        return device.getLastActive() > fiveMinutesAgo;
    }

    public Set<String> getAvailableDevicesForFile(String fileId) {
        return localFileRegistry.findDevicesWithVerifiedFile(fileId)
            .stream()
            .map(LocalFileRegistry::getDeviceId)
            .collect(java.util.stream.Collectors.toSet());
    }
} 