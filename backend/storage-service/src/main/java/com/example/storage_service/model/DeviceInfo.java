package com.example.storage_service.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.dataorchestrate.common.DeviceConfigUtil;
import java.util.Map;

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
} 