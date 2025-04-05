package com.example.storage_service.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Set;
import java.util.HashSet;
import java.util.Date;

@Document(collection = "file_distributions")
public class FileDistribution {
    @Id
    private String fileId;
    private String fileName;
    private String originalUploader;
    private Set<String> availableOnDevices;
    private Set<String> pendingDevices;
    private Date lastUpdated;
    private boolean isComplete;

    public FileDistribution(String fileId, String fileName, String originalUploader) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.originalUploader = originalUploader;
        this.availableOnDevices = new HashSet<>();
        this.pendingDevices = new HashSet<>();
        this.lastUpdated = new Date();
        this.isComplete = false;
    }

    // Getters and Setters
    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }
    
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    
    public String getOriginalUploader() { return originalUploader; }
    public void setOriginalUploader(String originalUploader) { this.originalUploader = originalUploader; }
    
    public Set<String> getAvailableOnDevices() { return availableOnDevices; }
    public void setAvailableOnDevices(Set<String> availableOnDevices) { this.availableOnDevices = availableOnDevices; }
    
    public Set<String> getPendingDevices() { return pendingDevices; }
    public void setPendingDevices(Set<String> pendingDevices) { this.pendingDevices = pendingDevices; }
    
    public Date getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Date lastUpdated) { this.lastUpdated = lastUpdated; }
    
    public boolean isComplete() { return isComplete; }
    public void setComplete(boolean complete) { isComplete = complete; }

    // Helper methods
    public void addAvailableDevice(String deviceId) {
        this.availableOnDevices.add(deviceId);
        this.pendingDevices.remove(deviceId);
        this.lastUpdated = new Date();
    }

    public void addPendingDevice(String deviceId) {
        if (!this.availableOnDevices.contains(deviceId)) {
            this.pendingDevices.add(deviceId);
            this.lastUpdated = new Date();
        }
    }

    public boolean needsReplication() {
        return !pendingDevices.isEmpty();
    }
} 