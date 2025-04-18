package com.mpjmp.gui;

public class SyncRule {
    private String id;
    private String ruleName;
    private SyncDirection direction;
    private boolean enabled;
    private String pathPattern;
    private String deviceId;
    private ConflictResolution conflictResolution;

    public SyncRule() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public SyncDirection getDirection() { return direction; }
    public void setDirection(SyncDirection direction) { this.direction = direction; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getPathPattern() { return pathPattern; }
    public void setPathPattern(String pathPattern) { this.pathPattern = pathPattern; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public ConflictResolution getConflictResolution() { return conflictResolution; }
    public void setConflictResolution(ConflictResolution conflictResolution) { this.conflictResolution = conflictResolution; }
}
