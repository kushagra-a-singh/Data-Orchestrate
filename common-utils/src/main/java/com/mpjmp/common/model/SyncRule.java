package com.mpjmp.common.model;

import lombok.Data;

@Data
public class SyncRule {
    private String pathPattern;
    private SyncDirection direction; // UPLOAD_ONLY, DOWNLOAD_ONLY, BIDIRECTIONAL
    private ConflictResolution resolution; // SERVER_WINS, CLIENT_WINS, NEWEST_WINS
    private boolean enabled;

    public enum SyncDirection { UPLOAD_ONLY, DOWNLOAD_ONLY, BIDIRECTIONAL }
    public enum ConflictResolution { SERVER_WINS, CLIENT_WINS, NEWEST_WINS }
}
