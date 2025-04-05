package com.example.storage_service.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "devices")
public class DeviceInfo {
    @Id
    private String id;
    private String ipAddress;
    private String hostname;
    private long lastActive;
    private boolean active;

    public DeviceInfo(String ipAddress, String hostname) {
        this.ipAddress = ipAddress;
        this.hostname = hostname;
        this.lastActive = System.currentTimeMillis();
        this.active = true;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    
    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }
    
    public long getLastActive() { return lastActive; }
    public void setLastActive(long lastActive) { this.lastActive = lastActive; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
