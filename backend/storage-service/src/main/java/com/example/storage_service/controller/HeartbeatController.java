package com.example.storage_service.controller;

import com.example.storage_service.model.DeviceInfo;
import com.example.storage_service.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;

@RestController
@RequestMapping("/api/heartbeat")
@RequiredArgsConstructor
public class HeartbeatController {
    private final DeviceRepository deviceRepository;

    @PostMapping("/{deviceId}")
    public void updateHeartbeat(@PathVariable String deviceId) {
        DeviceInfo device = deviceRepository.findById(deviceId)
            .orElseGet(() -> new DeviceInfo(deviceId, "UNKNOWN", "UNKNOWN", "OFFLINE", null, null));
        device.setLastSeen(Instant.now().toString());
        device.setStatus("ONLINE");
        deviceRepository.save(device);
    }
}
