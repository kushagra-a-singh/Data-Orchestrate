package com.mpjmp.storage.service;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "devices")
public class DeviceInfo {
    @Id
    private String id;
    private String ipAddress;
    private String hostname;
    private long lastActive;

    public DeviceInfo(String ipAddress, String hostname) {
        this.ipAddress = ipAddress;
        this.hostname = hostname;
        this.lastActive = System.currentTimeMillis();
    }
}
