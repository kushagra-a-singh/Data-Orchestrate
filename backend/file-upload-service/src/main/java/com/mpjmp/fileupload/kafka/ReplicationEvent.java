package com.mpjmp.fileupload.kafka;

import lombok.Data;
import java.util.List;

@Data
public class ReplicationEvent {
    private String fileId;
    private String fileName;
    private String uploaderDeviceId;
    private String uploaderDeviceName;
    private String downloadUrl;
    private List<String> targetDeviceIds;
    private String status; // INITIATED, STARTED, IN_PROGRESS, DONE, FAILED
    private String message;
    private String targetDeviceId; // For status updates
}
