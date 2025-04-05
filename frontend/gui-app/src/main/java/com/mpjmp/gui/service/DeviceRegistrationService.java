package com.mpjmp.gui.service;

import com.mpjmp.gui.model.DeviceInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.prefs.Preferences;

@Slf4j
public class DeviceRegistrationService {
    private static final String DEVICE_API = "http://localhost:8084/api/devices";
    private final RestTemplate restTemplate;
    private final Preferences preferences;
    private DeviceInfo deviceInfo;

    public DeviceRegistrationService() {
        this.restTemplate = new RestTemplate();
        this.preferences = Preferences.userRoot().node("com.mpjmp.gui");
        this.deviceInfo = loadOrRegisterDevice();
    }

    private DeviceInfo loadOrRegisterDevice() {
        String savedDeviceId = preferences.get("deviceId", null);
        
        if (savedDeviceId != null) {
            try {
                // Try to get existing device info
                DeviceInfo device = restTemplate.getForObject(
                    DEVICE_API + "/" + savedDeviceId,
                    DeviceInfo.class
                );
                
                if (device != null) {
                    // Update last seen timestamp
                    device.setLastSeen(new java.util.Date());
                    device.setActive(true);
                    return restTemplate.postForObject(DEVICE_API + "/update", device, DeviceInfo.class);
                }
            } catch (Exception e) {
                log.warn("Failed to load existing device info", e);
            }
        }

        // Create and register new device
        try {
            DeviceInfo newDevice = DeviceInfo.createNew();
            DeviceInfo registered = restTemplate.postForObject(DEVICE_API + "/register", newDevice, DeviceInfo.class);
            if (registered != null && registered.getDeviceId() != null) {
                preferences.put("deviceId", registered.getDeviceId());
                return registered;
            }
        } catch (Exception e) {
            log.error("Failed to register device", e);
        }

        // Fallback to local-only device info if registration fails
        DeviceInfo fallback = DeviceInfo.createNew();
        preferences.put("deviceId", fallback.getDeviceId());
        return fallback;
    }

    public String getDeviceId() {
        return deviceInfo.getDeviceId();
    }

    public DeviceInfo getDeviceInfo() {
        return deviceInfo;
    }

    public void updateLastSeen() {
        try {
            deviceInfo.setLastSeen(new java.util.Date());
            deviceInfo = restTemplate.postForObject(DEVICE_API + "/update", deviceInfo, DeviceInfo.class);
        } catch (Exception e) {
            log.error("Failed to update device last seen timestamp", e);
        }
    }

    public void deactivate() {
        try {
            deviceInfo.setActive(false);
            restTemplate.postForObject(DEVICE_API + "/update", deviceInfo, DeviceInfo.class);
        } catch (Exception e) {
            log.error("Failed to deactivate device", e);
        }
    }
} 