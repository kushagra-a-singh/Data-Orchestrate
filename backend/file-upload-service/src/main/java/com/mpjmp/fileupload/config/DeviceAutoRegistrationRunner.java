package com.mpjmp.fileupload.config;

import com.mpjmp.fileupload.model.Device;
import com.mpjmp.fileupload.repository.DeviceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

@Component
public class DeviceAutoRegistrationRunner implements CommandLineRunner {
    @Autowired
    private DeviceRepository deviceRepository;
    @Value("${app.device.id}")
    private String deviceId;
    @Value("${app.device.name:UNKNOWN}")
    private String deviceName;
    @Value("${app.device.ip:127.0.0.1}")
    private String deviceIp;
    @Override
    public void run(String... args) {
        if (!deviceRepository.existsById(deviceId)) {
            Device device = new Device();
            device.setDeviceId(deviceId);
            device.setDeviceName(deviceName);
            device.setIp(deviceIp);
            device.setRegisteredAt(DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
            device.setLastSeen(DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
            device.setStatus("ONLINE");
            deviceRepository.save(device);
        }
    }
}
