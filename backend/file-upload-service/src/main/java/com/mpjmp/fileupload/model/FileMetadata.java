package com.mpjmp.fileupload.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@Data
@Document(collection = "file_metadata")
public class FileMetadata {
    @Id
    private String id;
    private String fileName;
    private String originalFileName;
    private String contentType;
    private long size;
    private String sizeFormatted; // Will store formatted size (e.g., "1.5 MB")
    private String status;
    private String uploadedBy;
    private String deviceName;
    private String deviceIp;
    private String deviceId;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Kolkata")
    private LocalDateTime uploadedAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Kolkata")
    private LocalDateTime processedAt;
    
    private String errorMessage;
    private long version;
    private String extractedText;
    private Map<String, Object> metadata;
    private String fileId;
    private String fileType;
    private long fileSize;
    private String uploadPath;
    private String processedPath;
    private String storagePath;
    private LocalDateTime processedTime;
    private boolean isCompressed;
    private String compressionType;
    private long compressedSize;

    public void setSize(long size) {
        this.size = size;
        this.sizeFormatted = formatFileSize(size);
    }

    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
        }
    }
} 