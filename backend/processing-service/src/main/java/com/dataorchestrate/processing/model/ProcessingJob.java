package com.dataorchestrate.processing.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Document(collection = "processing_jobs")
public class ProcessingJob {
    @Id
    private String id;
    private String fileId;
    private String fileName;
    private String originalFileName;
    private String status; // PENDING, PROCESSING, COMPLETED, FAILED
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Map<String, Object> processingResult;
    private Map<String, Object> metadata;
} 