package com.example.storage_service.model;

import lombok.Data;

@Data
public class ReplicationRequest {
    private String fileId;
    private String fileName;
    private String sourceDeviceUrl;
    private String deviceId;
    private String originalFileName;
    // Add more fields as needed (e.g., sourceDeviceId)
}
