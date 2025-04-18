package com.mpjmp.orchestrator.service;

import com.mpjmp.orchestrator.model.DeviceInfo;
import com.mpjmp.orchestrator.repository.DeviceInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DeviceRegistryService {
    private static final Duration ONLINE_THRESHOLD = Duration.ofMinutes(2);
    private static final long HEARTBEAT_INTERVAL_MS = 30000; // 30 seconds

    @Autowired
    private DeviceInfoRepository deviceInfoRepository;

    // --- ENSURE DEVICE DISCOVERY FOR HTTP ORCHESTRATION ---
    // No Kafka logic to remove here, but ensure getOnlineDevices() and getOfflineDevices() are correct
    // Devices should have IPs set for HTTP communication
    public List<DeviceInfo> getOfflineDevices() {
        OffsetDateTime now = OffsetDateTime.now();
        return deviceInfoRepository.findAll().stream()
            .filter(device -> {
                try {
                    OffsetDateTime lastSeen = OffsetDateTime.parse(device.getLastSeen());
                    return Duration.between(lastSeen, now).compareTo(ONLINE_THRESHOLD) > 0;
                } catch (DateTimeParseException | NullPointerException e) {
                    return true; // Consider devices with invalid/empty lastSeen as offline
                }
            })
            .collect(Collectors.toList());
    }

    public void updateHeartbeat(String deviceId) {
        DeviceInfo device = deviceInfoRepository.findById(deviceId)
                .orElseGet(() -> new DeviceInfo(deviceId, "UNKNOWN", "UNKNOWN", "OFFLINE", null, null));
        device.setLastSeen(Instant.now().toString());
        device.setStatus("ONLINE");
        deviceInfoRepository.save(device);
    }

    public List<DeviceInfo> getOnlineDevices() {
        Instant now = Instant.now();
        List<DeviceInfo> allDevices = deviceInfoRepository.findAll();
        return allDevices.stream()
            .peek(device -> {
                try {
                    Instant lastSeen = Instant.parse(device.getLastSeen());
                    if (Duration.between(lastSeen, now).toMillis() > HEARTBEAT_INTERVAL_MS * 2) {
                        device.setStatus("OFFLINE");
                    } else {
                        device.setStatus("ONLINE");
                    }
                } catch (DateTimeParseException e) {
                    device.setStatus("OFFLINE");
                }
            })
            .collect(Collectors.toList());
    }
}
