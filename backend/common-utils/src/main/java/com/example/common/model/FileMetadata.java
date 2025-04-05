package com.example.common.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Represents metadata for a file in the system.
 * This entity stores information about uploaded files including their processing status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "files")
public class FileMetadata {
    
    public enum Status {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }

    @Id
    private String id;

    @NotBlank(message = "File name is required")
    @Indexed
    private String fileName;

    @NotBlank(message = "File type is required")
    private String fileType;

    @NotNull(message = "File size is required")
    @Min(value = 0, message = "File size must be positive")
    private Long fileSize;

    @NotNull(message = "Status is required")
    @Builder.Default
    private Status status = Status.PENDING;

    @NotBlank(message = "Storage path is required")
    private String storagePath;

    @NotNull(message = "Timestamp is required")
    @Builder.Default
    private Instant timestamp = Instant.now();

    private String extractedText;

    private Double compressionRatio;

    @Builder.Default
    private Long extractedTextLength = 0L;

    @Builder.Default
    private Integer processingAttempts = 0;

    private String errorMessage;

    private String filePath;

    private String contentType;

    private LocalDateTime uploadTime;

    private String uploadedBy;

    /**
     * Creates a new FileMetadata instance with basic file information.
     *
     * @param fileName    The name of the file
     * @param fileType    The MIME type of the file
     * @param fileSize    The size of the file in bytes
     * @param storagePath The path where the file is stored
     */
    public FileMetadata(String fileName, String fileType, long fileSize, String storagePath) {
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.storagePath = storagePath;
        this.status = Status.PENDING;
        this.timestamp = Instant.now();
        this.extractedText = null;
        this.compressionRatio = null;
        this.extractedTextLength = 0L;
        this.processingAttempts = 0;
    }

    /**
     * Increments the processing attempts counter.
     * @return the new number of attempts
     */
    public int incrementProcessingAttempts() {
        return ++this.processingAttempts;
    }

    /**
     * Updates the status and optionally sets an error message.
     *
     * @param status The new status
     * @param errorMessage Optional error message when status is FAILED
     */
    public void updateStatus(Status status, String errorMessage) {
        this.status = status;
        if (status == Status.FAILED) {
            this.errorMessage = errorMessage;
        }
    }

    public void setCompressionRatio(Double compressionRatio) {
        this.compressionRatio = compressionRatio;
    }

    public Double getCompressionRatio() {
        return compressionRatio;
    }

    public String getFileType() {
        return fileType;
    }

    public void setExtractedText(String extractedText) {
        this.extractedText = extractedText;
    }

    public String getExtractedText() {
        return extractedText;
    }

    public void setExtractedTextLength(long extractedTextLength) {
        this.extractedTextLength = extractedTextLength;
    }

    public long getExtractedTextLength() {
        return extractedTextLength;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setStatus(String status) {
        this.status = Status.valueOf(status);
    }

    public String getStatus() {
        return status.name();
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = Instant.ofEpochMilli(timestamp);
    }

    public long getTimestamp() {
        return timestamp.toEpochMilli();
    }
}
