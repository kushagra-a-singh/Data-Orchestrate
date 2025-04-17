package com.mpjmp.fileupload.config;

import com.mpjmp.fileupload.model.Device;
import com.mpjmp.fileupload.repository.DeviceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Component
public class DeviceHeartbeatConfig {
    @Autowired
    private DeviceRepository deviceRepository;
    @Value("${app.device.id}")
    private String deviceId;
    @Scheduled(fixedRate = 30000)
    public void updateHeartbeat() {
        Optional<Device> opt = deviceRepository.findById(deviceId);
        opt.ifPresent(device -> {
            device.setLastSeen(DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
            device.setStatus("ONLINE");
            deviceRepository.save(device);
        });
    }
}
