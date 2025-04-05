package com.example.fileuploadservice.service;

import com.example.common.model.FileMetadata;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface FileStorageService {
    /**
     * Store a file and return its metadata
     */
    FileMetadata storeFile(MultipartFile file);

    /**
     * Store a file asynchronously with additional metadata
     */
    CompletableFuture<FileMetadata> storeFileAsync(MultipartFile file, Map<String, String> metadata);

    /**
     * Get metadata for a specific file by ID
     */
    Optional<FileMetadata> getFileMetadata(String fileId);

    /**
     * List metadata for all stored files
     */
    List<FileMetadata> listFiles(FileMetadata.Status status);

    /**
     * Delete a file by ID
     */
    void deleteFile(String fileId);

    /**
     * Load a file as a resource
     */
    Resource loadFileAsResource(String fileId);
}