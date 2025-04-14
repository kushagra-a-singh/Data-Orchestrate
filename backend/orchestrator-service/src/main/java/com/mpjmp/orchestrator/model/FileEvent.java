package com.mpjmp.orchestrator.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Document(collection = "file_events")
public class FileEvent {
    @Id
    private String id;
    private String fileId;
    private String fileName;
    private String originalFileName;
    private String eventType; // UPLOAD, DELETE, STATUS_CHANGE
    private String status;
    private String errorMessage;
    private String uploadedBy;
    private LocalDateTime timestamp;
    private Map<String, Object> metadata;
} 