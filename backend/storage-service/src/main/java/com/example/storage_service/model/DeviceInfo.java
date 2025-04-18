package com.example.storage_service.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceInfo {
    private String deviceId;
    private String deviceName;
    private String deviceType;
    private String status;
    private String lastSeen;
    private String storagePath;

    public String getSyncUrl() {
        // Placeholder: Replace with actual logic to construct the sync URL
        return "http://" + deviceName + "/sync";
    }

    public boolean isOffline() {
        return "OFFLINE".equalsIgnoreCase(status);
    }

    public String getId() {
        return deviceId;
    }
} 