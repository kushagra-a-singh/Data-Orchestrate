package com.mpjmp.fileupload.service;

import com.dataorchestrate.common.DeviceConfigUtil;
import com.dataorchestrate.common.DeviceIdentifier;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mpjmp.common.model.FileMetadata;
import com.mpjmp.fileupload.repository.FileMetadataRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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

    private Map<String, String> selfDevice;
    private List<Map<String, String>> peerDevices;

    @PostConstruct
    private void initDeviceConfig() {
        selfDevice = DeviceConfigUtil.getSelfDevice();
        peerDevices = DeviceConfigUtil.getPeerDevices();
        if (selfDevice == null) {
            throw new RuntimeException("Could not identify self device from devices.json");
        }
    }

    public String getAbsoluteUploadDir() {
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
        // --- COLLISION HANDLING ---
        String fileNameToSave = originalFilename;
        Path candidatePath = uploadPath.resolve(fileNameToSave);
        int count = 1;
        String namePart = originalFilename;
        String extPart = "";
        int dotIdx = originalFilename.lastIndexOf('.');
        if (dotIdx > 0) {
            namePart = originalFilename.substring(0, dotIdx);
            extPart = originalFilename.substring(dotIdx);
        }
        while (Files.exists(candidatePath)) {
            fileNameToSave = String.format("%s (%d)%s", namePart, count, extPart);
            candidatePath = uploadPath.resolve(fileNameToSave);
            count++;
        }
        saveFile(file, uploadPath, fileNameToSave);
        Path savedFilePath = uploadPath.resolve(fileNameToSave);
        if (Files.exists(savedFilePath)) {
            log.info("File saved successfully at: {}", savedFilePath.toAbsolutePath());
        } else {
            log.error("File was NOT saved at: {}", savedFilePath.toAbsolutePath());
        }
        FileMetadata metadata = new FileMetadata();
        metadata.setFileName(fileNameToSave); // store actual saved file name
        metadata.setOriginalFileName(originalFilename);
        metadata.setContentType(file.getContentType());
        metadata.setFileSize(file.getSize());
        metadata.setStatus("UPLOADED");
        metadata.setUploadedBy(uploadedBy);
        metadata.setDeviceName(dynamicDeviceName);
        metadata.setDeviceId(dynamicDeviceId);
        metadata.setDeviceIp(dynamicDeviceIp);
        metadata.setUploadedAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
        metadata.setStoragePath(savedFilePath.toAbsolutePath().toString());
        FileMetadata savedMetadata = fileMetadataRepository.save(metadata);
        try {
            // Notify peer devices about the new file
            notifyPeerDevicesAboutNewFile(savedMetadata);
            
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

    /**
     * Get FileMetadata by stored (UUID) file name and deviceId
     */
    public FileMetadata getFileMetadataByStoredFileName(String deviceId, String fileName) {
        // Query by deviceId and fileName (UUID)
        org.springframework.data.mongodb.core.query.Query query = new org.springframework.data.mongodb.core.query.Query();
        query.addCriteria(org.springframework.data.mongodb.core.query.Criteria.where("deviceId").is(deviceId)
                .and("fileName").is(fileName));
        return mongoTemplate.findOne(query, FileMetadata.class);
    }

    /**
     * Find file metadata by original file name and deviceId
     * This is useful for replication when we only have the original filename
     */
    public FileMetadata findFileByOriginalName(String deviceId, String originalFileName) {
        org.springframework.data.mongodb.core.query.Query query = new org.springframework.data.mongodb.core.query.Query();
        query.addCriteria(org.springframework.data.mongodb.core.query.Criteria.where("deviceId").is(deviceId)
                .and("originalFileName").is(originalFileName));
        return mongoTemplate.findOne(query, FileMetadata.class);
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

    /**
     * Notify peer devices about a new file that was uploaded
     * This triggers replication to other devices in the network
     */
    private void notifyPeerDevicesAboutNewFile(FileMetadata metadata) {
        try {
            List<String> peerUrls = getPeerDeviceUrls();
            Map<String, Object> replicationRequest = new HashMap<>();
            replicationRequest.put("fileId", metadata.getFileName());
            replicationRequest.put("fileName", metadata.getFileName());
            replicationRequest.put("originalFileName", metadata.getOriginalFileName());
            replicationRequest.put("deviceId", metadata.getDeviceId());
            replicationRequest.put("sourceDeviceUrl", getSelfDeviceUrl());

            // --- Read file content and encode as Base64 ---
            String absUploadDir = getAbsoluteUploadDir();
            Path filePath = Paths.get(absUploadDir, metadata.getDeviceId(), metadata.getFileName());
            if (!Files.exists(filePath)) {
                log.error("[REPLICATION] File not found for replication at {}", filePath.toAbsolutePath());
                return;
            }
            byte[] fileBytes = Files.readAllBytes(filePath);
            String fileContentBase64 = java.util.Base64.getEncoder().encodeToString(fileBytes);
            replicationRequest.put("content", fileContentBase64);

            String requestJson = objectMapper.writeValueAsString(replicationRequest);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);
            RestTemplate restTemplate = new RestTemplate();
            for (String peerUrl : peerUrls) {
                try {
                    String replicationEndpoint = peerUrl + "/replicate-file";
                    log.info("Sending replication request to peer device: {}", replicationEndpoint);
                    restTemplate.postForEntity(replicationEndpoint, entity, String.class);
                    log.info("Replication request sent to {}", peerUrl);
                } catch (Exception e) {
                    log.error("Failed to send replication request to peer device {}: {}", peerUrl, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error notifying peer devices about new file: {}", e.getMessage(), e);
        }
    }

    private String getSelfDeviceUrl() {
        return "http://" + selfDevice.get("ip") + ":" + selfDevice.get("storage_port");
    }

    private List<String> getPeerDeviceUrls() {
        List<String> urls = new ArrayList<>();
        for (Map<String, String> d : peerDevices) {
            String ip = d.get("ip");
            String port = d.get("storage_port");
            if (ip == null || port == null) {
                log.warn("Skipping peer device with missing ip or storage_port: {}", d);
                continue;
            }
            urls.add("http://" + ip + ":" + port);
        }
        return urls;
    }
}