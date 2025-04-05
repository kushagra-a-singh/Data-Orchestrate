package com.example.storage_service;

import com.example.storage_service.model.DeviceInfo;
import com.example.storage_service.repository.DeviceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.InetAddress;

@Service
public class DeviceRegistryService {
    
    private final DeviceRepository deviceRepository;

    @Value("${server.port}")
    private int serverPort;

    public DeviceRegistryService(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    public void registerDevice() {
        try {
            String ip = InetAddress.getLocalHost().getHostAddress();
            String hostname = InetAddress.getLocalHost().getHostName();
            DeviceInfo device = new DeviceInfo(ip + ":" + serverPort, hostname);
            deviceRepository.save(device);
            System.out.println("Registered Device: " + ip + ":" + serverPort);
        } catch (Exception e) {
            System.err.println("Failed to register device: " + e.getMessage());
        }
    }
}
