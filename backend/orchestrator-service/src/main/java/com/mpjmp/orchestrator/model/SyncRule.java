package com.mpjmp.orchestrator.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "sync_rules")
public class SyncRule {
    @Id
    private String id;
    private String ruleName;
    private SyncDirection direction;
    private boolean enabled;
    private String pathPattern;
    private String deviceId;

    public boolean isEnabled() { return enabled; }
    public String getPathPattern() { return pathPattern; }
    public SyncDirection getDirection() { return direction; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
}
