package com.mpjmp.common.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

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
    private String status;
    private String errorMessage;
} 