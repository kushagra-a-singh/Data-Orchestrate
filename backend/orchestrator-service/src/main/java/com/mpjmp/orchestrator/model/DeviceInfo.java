package com.mpjmp.orchestrator.model;

import com.dataorchestrate.common.DeviceConfigUtil;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

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
        Map<String, String> device = DeviceConfigUtil.getDeviceByName(deviceName);
        if (device != null) {
            return "http://" + device.get("ip") + ":" + device.get("port") + "/sync";
        }
        return null;
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
        Map<String, String> device = DeviceConfigUtil.getDeviceByName(deviceName);
        if (device != null) {
            return "http://" + device.get("ip") + ":" + device.get("port") + "/api/health";
        }
        return null;
    }
}
