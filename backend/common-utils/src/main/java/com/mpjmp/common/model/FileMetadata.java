package com.mpjmp.common.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileMetadata {
    private String fileId;
    private String fileName;
    private String fileType;
    private long fileSize;
    private String uploadPath;
    private String processedPath;
    private String storagePath;
    private LocalDateTime uploadTime;
    private LocalDateTime processedTime;
    private String deviceId;
    private String deviceName;
    private String status;
    private String uploadedBy;
    private LocalDateTime uploadedAt;
    private LocalDateTime processedAt;
    private String extractedText;
    private Map<String, Object> metadata;
    private String contentType;
    private String originalFileName;
    private boolean isCompressed;
    private String compressionType;
    private long compressedSize;
    private String deviceIp;
    private String sizeFormatted;
    private long version;
    private String errorMessage;
}