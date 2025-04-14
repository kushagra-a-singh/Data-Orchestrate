package com.example.storage_service.service;

import com.example.storage_service.model.DeviceInfo;
import com.example.storage_service.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageService {
    private final DeviceRepository deviceRepository;

    @Value("${storage.dir}")
    private String storageDir;

    public String storeFile(MultipartFile file, String deviceId) {
        try {
            DeviceInfo device = deviceRepository.findById(deviceId)
                    .orElseThrow(() -> new RuntimeException("Device not found: " + deviceId));

            Path devicePath = Paths.get(storageDir, deviceId);
            Files.createDirectories(devicePath);

            Path filePath = devicePath.resolve(file.getOriginalFilename());
            Files.copy(file.getInputStream(), filePath);

            log.info("File stored successfully: {} for device: {}", filePath, deviceId);
            return filePath.toString();
        } catch (IOException e) {
            log.error("Error storing file for device {}: {}", deviceId, e.getMessage());
            throw new RuntimeException("Failed to store file", e);
        }
    }

    public List<DeviceInfo> getAllDevices() {
        log.info("Retrieving all devices");
        return deviceRepository.findAll();
    }

    public DeviceInfo getDevice(String deviceId) {
        log.info("Retrieving device: {}", deviceId);
        return deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found: " + deviceId));
    }
} 