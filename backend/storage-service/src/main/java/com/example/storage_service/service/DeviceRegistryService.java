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
} 