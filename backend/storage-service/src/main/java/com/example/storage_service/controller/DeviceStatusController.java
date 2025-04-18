package com.example.storage_service.controller;

import com.example.storage_service.model.DeviceInfo;
import com.example.storage_service.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceStatusController {
    private final DeviceRepository deviceRepository;

    @GetMapping("")
    public List<DeviceInfo> getAllDevices() {
        return deviceRepository.findAll();
    }

    @GetMapping("/{deviceId}")
    public DeviceInfo getDevice(@PathVariable String deviceId) {
        return deviceRepository.findById(deviceId).orElse(null);
    }
}
