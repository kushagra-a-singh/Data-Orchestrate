package com.dataorchestrate.processing.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Document(collection = "file_metadata")
public class FileMetadata {
    @Id
    private String id;
    private String fileId;
    private String fileName;
    private String fileType;
    private long fileSize;
    private String deviceId;
    private String deviceName;
    private String status;
    private String uploadedBy;
    private LocalDateTime uploadTime;
    private LocalDateTime processTime;
    private String sizeFormatted;
    private String originalFileName;
    private String contentType;
    private String errorMessage;
    private long version;
    private String extractedText;
    private Map<String, Object> metadata;
    private String storagePath;
    private String uploadPath;
    private String processedPath;
    private LocalDateTime processedAt;
    private boolean isCompressed;
    private String compressionType;
    private long compressedSize;
    private String deviceIp;
}