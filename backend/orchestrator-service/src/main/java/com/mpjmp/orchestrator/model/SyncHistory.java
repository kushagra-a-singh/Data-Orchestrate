package com.mpjmp.orchestrator.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "sync_history")
public class SyncHistory {
    @Id
    private String id;
    private String deviceId;
    private String fileId;
    private LocalDateTime timestamp;
    private String sourceDeviceId;
    private String targetDeviceId;
    private LocalDateTime syncTime;
    private String status;

    public SyncHistory() {}

    public SyncHistory(String deviceId, String fileId, LocalDateTime timestamp) {
        this.deviceId = deviceId;
        this.fileId = fileId;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getSourceDeviceId() { return sourceDeviceId; }
    public void setSourceDeviceId(String sourceDeviceId) { this.sourceDeviceId = sourceDeviceId; }
    public String getTargetDeviceId() { return targetDeviceId; }
    public void setTargetDeviceId(String targetDeviceId) { this.targetDeviceId = targetDeviceId; }
    public LocalDateTime getSyncTime() { return syncTime; }
    public void setSyncTime(LocalDateTime syncTime) { this.syncTime = syncTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
