package com.example.storage_service.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Document(collection = "local_files")
public class LocalFileRegistry {
    @Id
    private String id;
    private String deviceId;
    private String fileId;
    private String fileName;
    private String filePath;
    private long fileSize;
    private Date storedAt;
    private boolean verified;
    private int verificationAttempts;
    private Date lastVerified;

    public LocalFileRegistry(String deviceId, String fileId, String fileName, String filePath, long fileSize) {
        this.deviceId = deviceId;
        this.fileId = fileId;
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.storedAt = new Date();
        this.verified = true;
        this.verificationAttempts = 0;
        this.lastVerified = new Date();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public Date getStoredAt() { return storedAt; }
    public void setStoredAt(Date storedAt) { this.storedAt = storedAt; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public int getVerificationAttempts() { return verificationAttempts; }
    public void setVerificationAttempts(int verificationAttempts) { 
        this.verificationAttempts = verificationAttempts; 
    }

    public Date getLastVerified() { return lastVerified; }
    public void setLastVerified(Date lastVerified) { this.lastVerified = lastVerified; }

    public void incrementVerificationAttempts() {
        this.verificationAttempts++;
        this.lastVerified = new Date();
    }
} 