package com.mpjmp.fileupload.service;

import com.dataorchestrate.common.DeviceIdentifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mpjmp.fileupload.model.FileMetadata;
import com.mpjmp.fileupload.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final FileMetadataRepository fileMetadataRepository;
    private final ObjectMapper objectMapper;
    private final MongoTemplate mongoTemplate;

    @Value("${app.upload.dir}")
    private String uploadDir;

    private String getAbsoluteUploadDir() {
        // Always use an absolute path for uploads
        Path absPath = Paths.get(uploadDir).toAbsolutePath();
        if (!Files.exists(absPath)) {
            try {
                Files.createDirectories(absPath);
                log.info("Created upload directory: {}", absPath);
            } catch (IOException e) {
                log.error("Failed to create upload directory: {}", absPath, e);
            }
        }
        return absPath.toString();
    }

    public FileMetadata uploadFile(MultipartFile file, String uploadedBy, String deviceName, String deviceIp) throws IOException {
        DeviceIdentifier deviceIdentifier = new DeviceIdentifier();
        String dynamicDeviceId = deviceIdentifier.getDeviceId();
        String dynamicDeviceName = deviceIdentifier.getDeviceId(); // Or getName() if available
        String dynamicDeviceIp = deviceIp != null ? deviceIp : InetAddress.getLocalHost().getHostAddress();
        String absUploadDir = getAbsoluteUploadDir();
        Path uploadPath = Paths.get(absUploadDir, dynamicDeviceId);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = UUID.randomUUID().toString() + fileExtension;
        saveFile(file, uploadPath, fileName);
        Path savedFilePath = uploadPath.resolve(fileName);
        if (Files.exists(savedFilePath)) {
            log.info("File saved successfully at: {}", savedFilePath.toAbsolutePath());
        } else {
            log.error("File was NOT saved at: {}", savedFilePath.toAbsolutePath());
        }
        FileMetadata metadata = new FileMetadata();
        metadata.setFileName(fileName);
        metadata.setOriginalFileName(originalFilename);
        metadata.setContentType(file.getContentType());
        metadata.setSize(file.getSize());
        metadata.setStatus("UPLOADED");
        metadata.setUploadedBy(uploadedBy);
        metadata.setDeviceName(dynamicDeviceName);
        metadata.setDeviceId(dynamicDeviceId);
        metadata.setDeviceIp(dynamicDeviceIp);
        metadata.setUploadedAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
        metadata.setStoragePath(savedFilePath.toAbsolutePath().toString());
        FileMetadata savedMetadata = fileMetadataRepository.save(metadata);
        try {
            // TODO: Implement HTTP-based event notifications or calls
            savedMetadata.setStatus("UPLOADED");
            savedMetadata = fileMetadataRepository.save(savedMetadata);
            // TODO: Implement HTTP-based notification
            log.info("Notification sent for file upload: {}", originalFilename);
        } catch (Exception e) {
            savedMetadata.setStatus("FAILED");
            savedMetadata.setErrorMessage("Failed to process file: " + e.getMessage());
            savedMetadata = fileMetadataRepository.save(savedMetadata);
            // TODO: Implement HTTP-based notification
            log.error("Notification sent for FAILED file upload: {}", originalFilename);
            throw new RuntimeException("Failed to process file", e);
        }
        return savedMetadata;
    }

    private void saveFile(MultipartFile file, Path uploadPath, String fileName) throws IOException {
        try {
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);
            log.info("File saved successfully: {} at {}", fileName, filePath.toAbsolutePath());
        } catch (IOException e) {
            log.error("Error saving file: {} at {}", fileName, uploadPath.toAbsolutePath(), e);
            throw e;
        }
    }

    public FileMetadata getFileMetadata(String fileId) {
        return fileMetadataRepository.findById(fileId).orElse(null);
    }

    public List<FileMetadata> listFiles(String status, String uploadedBy) {
        Query query = new Query();
        
        if (status != null && !status.isEmpty()) {
            query.addCriteria(Criteria.where("status").is(status));
        }
        
        if (uploadedBy != null && !uploadedBy.isEmpty()) {
            query.addCriteria(Criteria.where("uploadedBy").is(uploadedBy));
        }
        
        return mongoTemplate.find(query, FileMetadata.class);
    }

    public boolean deleteFile(String fileId) {
        try {
            FileMetadata metadata = fileMetadataRepository.findById(fileId).orElse(null);
            if (metadata == null) {
                return false;
            }

            // Delete the physical file
            Path filePath = Paths.get(uploadDir, metadata.getDeviceId(), metadata.getFileName());
            Files.deleteIfExists(filePath);

            // Delete metadata from MongoDB
            fileMetadataRepository.deleteById(fileId);

            // TODO: Implement HTTP-based notification
            return true;
        } catch (Exception e) {
            log.error("Error deleting file: " + fileId, e);
            // TODO: Implement HTTP-based notification
            throw new RuntimeException("Failed to delete file", e);
        }
    }

    public FileMetadata updateFileStatus(String fileId, String newStatus, String errorMessage) {
        try {
            FileMetadata metadata = fileMetadataRepository.findById(fileId).orElse(null);
            if (metadata != null) {
                String oldStatus = metadata.getStatus();
                metadata.setStatus(newStatus);
                metadata.setErrorMessage(errorMessage);
                if (newStatus.equals("COMPLETED")) {
                    metadata.setProcessedAt(LocalDateTime.now());
                }
                fileMetadataRepository.save(metadata);

                // TODO: Implement HTTP-based event notifications or calls
                // TODO: Implement HTTP-based notification
                return metadata;
            }
            return null;
        } catch (Exception e) {
            log.error("Error updating file status: " + fileId, e);
            // TODO: Implement HTTP-based notification
            throw new RuntimeException("Failed to update file status", e);
        }
    }

    public FileMetadata updateFileMetadata(FileMetadata metadata) {
        return fileMetadataRepository.save(metadata);
    }
}