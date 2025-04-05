package com.example.fileuploadservice;

import com.example.common.model.FileMetadata;
import com.example.common.repository.FileMetadataRepository;
import com.example.fileuploadservice.config.FileUploadProperties;
import com.example.fileuploadservice.exception.FileNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {
    private final FileUploadProperties properties;
    private final FileMetadataRepository fileMetadataRepository;
    private final KafkaTemplate<String, FileMetadata> kafkaTemplate;

    @Override
    public FileMetadata storeFile(MultipartFile file) {
        try {
            Path uploadDir = Paths.get(properties.getDir());
            if (!Files.exists(uploadDir)) {
                log.info("Creating upload directory: {}", uploadDir);
                Files.createDirectories(uploadDir);
            }

            String fileId = UUID.randomUUID().toString();
            String fileName = file.getOriginalFilename();
            Path targetPath = uploadDir.resolve(fileId);

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            FileMetadata fileMetadata = FileMetadata.builder()
                .id(fileId)
                .fileName(fileName)
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .storagePath(targetPath.toString())
                .uploadTime(LocalDateTime.now())
                .status(FileMetadata.Status.PENDING)
                .build();

            FileMetadata savedMetadata = fileMetadataRepository.save(fileMetadata);
            kafkaTemplate.send("file-upload", fileId, savedMetadata);
            return savedMetadata;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    @Override
    public CompletableFuture<FileMetadata> storeFileAsync(MultipartFile file, Map<String, String> metadata) {
        return CompletableFuture.supplyAsync(() -> storeFile(file));
    }

    @Override
    public Resource loadFileAsResource(String fileId) {
        try {
            Optional<FileMetadata> metadata = getFileMetadata(fileId);
            if (metadata.isEmpty()) {
                throw new FileNotFoundException("File not found with id: " + fileId);
            }

            Path filePath = Paths.get(metadata.get().getStoragePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return resource;
            } else {
                throw new FileNotFoundException("File not found: " + fileId);
            }
        } catch (MalformedURLException e) {
            throw new FileNotFoundException("File not found: " + fileId, e);
        }
    }

    @Override
    public Optional<FileMetadata> getFileMetadata(String fileId) {
        return fileMetadataRepository.findById(fileId);
    }

    @Override
    public List<FileMetadata> listFiles(FileMetadata.Status status) {
        if (status == null) {
            return fileMetadataRepository.findAll();
        }
        return fileMetadataRepository.findAll().stream()
            .filter(f -> f.getStatus().equals(status))
            .toList();
    }

    @Override
    public void deleteFile(String fileId) {
        Optional<FileMetadata> metadata = getFileMetadata(fileId);
        if (metadata.isEmpty()) {
            throw new FileNotFoundException("File not found with id: " + fileId);
        }

        try {
            Path filePath = Paths.get(metadata.get().getStoragePath());
            Files.deleteIfExists(filePath);
            fileMetadataRepository.deleteById(fileId);
            log.info("File deleted successfully: {}", fileId);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file: " + fileId, e);
        }
    }
} 