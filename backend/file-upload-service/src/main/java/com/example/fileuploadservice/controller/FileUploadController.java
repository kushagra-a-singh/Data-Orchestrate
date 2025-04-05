package com.example.fileuploadservice.controller;

import com.example.common.model.FileMetadata;
import com.example.fileuploadservice.config.FileUploadProperties;
import com.example.fileuploadservice.exception.FileUploadException;
import com.example.fileuploadservice.exception.FileValidationException;
import com.example.fileuploadservice.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Validated
@Tag(name = "File Upload", description = "File upload and management endpoints")
public class FileUploadController {

    private final FileStorageService fileStorageService;
    private final FileUploadProperties properties;

    @Operation(summary = "Get service status", description = "Check if the file upload service is running")
    @ApiResponse(responseCode = "200", description = "Service is running")
    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getStatus() {
        return ResponseEntity.ok(Map.of(
            "status", "running",
            "service", "file-upload",
            "maxFileSize", properties.getMaxFileSize() + " bytes",
            "allowedTypes", String.join(", ", properties.getAllowedTypes())
        ));
    }

    @Operation(summary = "Upload a file", description = "Upload a file for processing")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "File uploaded successfully",
                    content = @Content(schema = @Schema(implementation = FileMetadata.class))),
        @ApiResponse(responseCode = "400", description = "Invalid file or request"),
        @ApiResponse(responseCode = "413", description = "File too large"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CompletableFuture<ResponseEntity<FileMetadata>> uploadFile(
            @Parameter(description = "File to upload", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Additional metadata")
            @RequestParam(required = false) Map<String, String> metadata) {
        
        log.info("Received file upload request: {}, size: {}", file.getOriginalFilename(), file.getSize());
        
        validateFile(file);
        
        return fileStorageService.storeFileAsync(file, metadata)
            .thenApply(ResponseEntity::ok)
            .exceptionally(e -> {
                log.error("File upload failed", e);
                throw new FileUploadException("Failed to upload file: " + e.getMessage());
            });
    }

    @Operation(summary = "Get file metadata", description = "Get metadata for a specific file")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "File metadata retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "File not found")
    })
    @GetMapping("/{fileId}")
    public ResponseEntity<FileMetadata> getFileMetadata(@PathVariable String fileId) {
        return fileStorageService.getFileMetadata(fileId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "List uploaded files", description = "Get a list of all uploaded files")
    @GetMapping
    public ResponseEntity<List<FileMetadata>> listFiles(
            @Parameter(description = "Filter by status")
            @RequestParam(required = false) FileMetadata.Status status) {
        return ResponseEntity.ok(fileStorageService.listFiles(status));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a file")
    @ApiResponse(responseCode = "204", description = "File deleted successfully")
    @ApiResponse(responseCode = "404", description = "File not found")
    public ResponseEntity<Void> deleteFile(@PathVariable String id) {
        fileStorageService.deleteFile(id);
        return ResponseEntity.noContent().build();
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new FileValidationException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !properties.getAllowedTypes().contains(contentType)) {
            throw new FileValidationException("Invalid file type: " + contentType);
        }

        if (file.getSize() > properties.getMaxFileSize()) {
            throw new FileValidationException("File size exceeds maximum allowed size of " + 
                properties.getMaxFileSize() + " bytes");
        }
    }
}