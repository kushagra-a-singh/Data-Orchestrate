package com.example.storage_service.service;

import com.example.storage_service.model.DeviceInfo;
import com.example.storage_service.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceRegistryService {
    private final DeviceRepository deviceRepository;

    public DeviceInfo registerDevice(DeviceInfo deviceInfo) {
        log.info("Registering device: {}", deviceInfo.getDeviceId());
        return deviceRepository.save(deviceInfo);
    }

    public DeviceInfo updateDeviceStatus(String deviceId, String status) {
        log.info("Updating device status: {} to {}", deviceId, status);
        DeviceInfo device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found: " + deviceId));
        device.setStatus(status);
        return deviceRepository.save(device);
    }

    // --- ADDED: Get upload service URL for a deviceId ---
    public String getUploadServiceUrl(String deviceId) {
        // TODO: Ideally fetch from DB or config. For now, hardcode for demo.
        // Kushagra (your machine): DEVICE-Inspiron-7415-2244B2DE3179 -> http://192.168.1.6:8081
        // Anil Cerejo: DEVICE-AnilCerejo-SOMEID -> http://192.168.1.7:8081
        if ("DEVICE-Inspiron-7415-2244B2DE3179".equals(deviceId)) {
            return "http://192.168.1.6:8081";
        } else if ("DEVICE-AnilCerejo-SOMEID".equals(deviceId)) {
            return "http://192.168.1.7:8081";
        }
        // Fallback: null or throw error
        return null;
    }
} 