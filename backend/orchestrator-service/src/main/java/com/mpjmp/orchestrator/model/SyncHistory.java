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
}
