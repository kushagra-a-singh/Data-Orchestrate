package com.mpjmp.orchestrator.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "pending_syncs")
public class PendingSync {
    @Id
    private String id;
    private String deviceId;
    private String fileId;
    private String filename;
    private long timestamp;
    // Add more fields as needed

    public PendingSync() {}
    public PendingSync(String deviceId, String fileId, String filename, long timestamp) {
        this.deviceId = deviceId;
        this.fileId = fileId;
        this.filename = filename;
        this.timestamp = timestamp;
    }
}
