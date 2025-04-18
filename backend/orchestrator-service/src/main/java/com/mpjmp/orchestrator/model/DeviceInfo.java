package com.mpjmp.orchestrator.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "devices")
public class DeviceInfo {
    @Id
    private String deviceId;
    private String deviceName;
    private String deviceType;
    private String status;
    private String lastSeen;
    private String storagePath;
    private boolean online;

    public DeviceInfo(String deviceId, String deviceName, String deviceType, String status, String lastSeen, String storagePath) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.deviceType = deviceType;
        this.status = status;
        this.lastSeen = lastSeen;
        this.storagePath = storagePath;
    }

    public String getSyncUrl() {
        return "http://" + deviceName + "/sync";
    }

    public boolean isOffline() {
        return "OFFLINE".equalsIgnoreCase(status);
    }

    public String getId() {
        return deviceId;
    }

    public boolean isOnline() { 
        return online; 
    }

    public void setOnline(boolean online) { 
        this.online = online; 
    }

    public String getHealthCheckUrl() {
        return "http://" + deviceName + ":8081/api/health";
    }
}
