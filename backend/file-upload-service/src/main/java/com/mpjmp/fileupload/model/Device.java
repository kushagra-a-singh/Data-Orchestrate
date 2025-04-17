package com.mpjmp.fileupload.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "devices")
public class Device {
    @Id
    private String deviceId;
    private String deviceName;
    private String ip;
    private String status; // ONLINE, OFFLINE
    private String lastSeen;
    private String registeredAt;
}
