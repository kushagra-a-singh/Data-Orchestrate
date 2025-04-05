package com.mpjmp.gui.model;

import lombok.Data;
import java.util.Date;

@Data
public class DeviceInfo {
    private String deviceId;
    private String ipAddress;
    private String hostname;
    private Date lastSeen;
    private boolean active;

    public DeviceInfo(String ipAddress, String hostname) {
        this.deviceId = java.util.UUID.randomUUID().toString();
        this.ipAddress = ipAddress;
        this.hostname = hostname;
        this.lastSeen = new Date();
        this.active = true;
    }

    public static DeviceInfo createNew() {
        try {
            String ip = java.net.InetAddress.getLocalHost().getHostAddress();
            String hostname = java.net.InetAddress.getLocalHost().getHostName();
            return new DeviceInfo(ip, hostname);
        } catch (Exception e) {
            return new DeviceInfo("unknown", "unknown");
        }
    }
} 