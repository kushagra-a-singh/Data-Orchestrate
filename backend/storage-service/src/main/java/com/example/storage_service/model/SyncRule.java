package com.example.storage_service.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "sync_rules")
public class SyncRule {
    @Id
    private String id;
    private String deviceId;
    private String path;
    private String direction;
    private String conflictResolution;

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public String getConflictResolution() { return conflictResolution; }
    public void setConflictResolution(String conflictResolution) { this.conflictResolution = conflictResolution; }
}
