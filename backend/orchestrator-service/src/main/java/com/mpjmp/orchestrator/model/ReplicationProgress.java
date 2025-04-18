package com.mpjmp.orchestrator.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document(collection = "replication_progress")
public class ReplicationProgress {
    @Id
    private String id;
    private String fileId;
    private String deviceId;
    private double progress;
    private Instant lastUpdated;

    public ReplicationProgress() {}

    public ReplicationProgress(String fileId, String deviceId, double progress, Instant lastUpdated) {
        this.fileId = fileId;
        this.deviceId = deviceId;
        this.progress = progress;
        this.lastUpdated = lastUpdated;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public double getProgress() { return progress; }
    public void setProgress(double progress) { this.progress = progress; }
    public Instant getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }
}
