package com.example.fileuploadservice;

import com.example.common.model.FileMetadata;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface FileStorageService {
    FileMetadata storeFile(MultipartFile file);
    CompletableFuture<FileMetadata> storeFileAsync(MultipartFile file, Map<String, String> metadata);
    Resource loadFileAsResource(String fileId);
    Optional<FileMetadata> getFileMetadata(String fileId);
    List<FileMetadata> listFiles(FileMetadata.Status status);
    void deleteFile(String fileId);
}
