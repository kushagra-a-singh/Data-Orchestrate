package com.mpjmp.gui;

public class DeviceStatus {
    private final String deviceName;
    private final String deviceId;
    private final String status;
    private final String reachable;

    public DeviceStatus(String deviceName, String deviceId, String status, String reachable) {
        this.deviceName = deviceName;
        this.deviceId = deviceId;
        this.status = status;
        this.reachable = reachable;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getStatus() {
        return status;
    }

    public String getReachable() {
        return reachable;
    }
}
