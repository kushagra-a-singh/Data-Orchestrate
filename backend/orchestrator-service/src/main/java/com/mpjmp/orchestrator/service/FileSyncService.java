package com.mpjmp.orchestrator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mpjmp.common.model.FileMetadata;
import com.mpjmp.orchestrator.model.DeviceInfo;
import com.mpjmp.orchestrator.model.FileChangeEvent;
import com.mpjmp.orchestrator.model.SyncDirection;
import com.mpjmp.orchestrator.model.SyncHistory;
import com.mpjmp.orchestrator.model.SyncRule;
import com.mpjmp.orchestrator.repository.SyncRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.changestream.OperationType;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.GridFSBuckets;
import org.bson.types.ObjectId;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.mpjmp.orchestrator.repository.DeviceInfoRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileSyncService {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final MongoTemplate mongoTemplate;
    private final SyncRuleRepository ruleRepository;
    private final DeviceInfoRepository deviceInfoRepository;

    @Value("${app.sync.dir}")
    private String syncDir;

    @Value("${app.device.id}")
    private String deviceId;

    // Track file versions to handle conflicts
    private final ConcurrentHashMap<String, Long> fileVersions = new ConcurrentHashMap<>();

    private final Map<String, Long> lastReplicated = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> currentBatch = new ConcurrentHashMap<>();

    @PostConstruct
    public void initChangeStream() {
        new Thread(() -> {
            try {
                mongoTemplate.getCollection("file_metadata")
                    .watch()
                    .forEach(event -> {
                        if (event.getOperationType() == OperationType.INSERT) {
                            Document fileDoc = event.getFullDocument();
                            replicateFileToDevices(fileDoc.getString("fileId"));
                        }
                    });
            } catch (Exception e) {
                log.error("Change stream error: {}", e.getMessage());
            }
        }).start();
    }

    public void replicateFileToDevices(String fileId) {
        try {
            // --- Fetch metadata from file-upload-service via HTTP, not MongoDB ---
            String metadataUrl = String.format("http://localhost:8081/api/files/metadata/%s", fileId);
            ResponseEntity<FileMetadata> response = restTemplate.getForEntity(metadataUrl, FileMetadata.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.error("Metadata not found for file: {} via file-upload-service API", fileId);
                return;
            }
            FileMetadata fileMetadata = response.getBody();
            String uuidFileName = fileMetadata.getFileName();
            if (uuidFileName == null) {
                log.error("Could not determine UUID file name for fileId: {}. fileMetadata: {}", fileId, fileMetadata);
                return;
            }
            List<DeviceInfo> devices = deviceInfoRepository.findAll();
            for (DeviceInfo device : devices) {
                if (!device.getDeviceId().equals(deviceId) && device.isOnline()) {
                    uploadFileToDevice(fileId, uuidFileName, device);
                }
            }
        } catch (Exception e) {
            log.error("Error replicating file to devices: {}", e.getMessage());
        }
    }

    private void uploadFileToDevice(String fileId, String fileName, DeviceInfo device) {
        try {
            Path filePath = Paths.get(syncDir, fileName);
            byte[] fileContent = Files.readAllBytes(filePath);
            String uploadUrl = String.format("http://%s:8081/api/files/replicate", device.getDeviceName());
            Map<String, Object> request = new HashMap<>();
            request.put("fileId", fileId);
            request.put("fileName", fileName); // Always UUID
            request.put("content", fileContent);
            request.put("version", fileVersions.get(fileId));
            restTemplate.postForObject(uploadUrl, request, Void.class);
            log.info("Replicated file {} to device {}", fileName, device.getDeviceId());
        } catch (Exception e) {
            log.error("Failed to replicate file {} to device {}: {}", fileName, device.getDeviceId(), e.getMessage());
        }
    }

    public void handleFileConflict(String fileId, String fileName, String sourceDeviceId, long sourceVersion) {
        long localVersion = fileVersions.getOrDefault(fileId, 0L);
        
        if (sourceVersion > localVersion) {
            // Remote version is newer, download it
            try {
                downloadFile(fileId, fileName, sourceDeviceId);
                fileVersions.put(fileId, sourceVersion);
                log.info("Resolved conflict for file {}: using version from device {}", fileName, sourceDeviceId);
            } catch (Exception e) {
                log.error("Error resolving file conflict", e);
            }
        } else if (sourceVersion < localVersion) {
            // Local version is newer, send it to the source device
            try {
                // There is no uploadFile method; replicate using uploadFileToDevice
                Optional<DeviceInfo> deviceOpt = deviceInfoRepository.findById(sourceDeviceId);
                if (deviceOpt.isPresent()) {
                    uploadFileToDevice(fileId, fileName, deviceOpt.get());
                    log.info("Resolved conflict for file {}: sent local version to device {}", fileName, sourceDeviceId);
                } else {
                    log.error("Device not found for conflict upload: {}", sourceDeviceId);
                }
            } catch (Exception e) {
                log.error("Error sending local version to resolve conflict", e);
            }
        } else {
            // Versions are the same, no action needed
            log.info("No conflict resolution needed for file {}: versions are identical", fileName);
        }
    }

    private void downloadFile(String fileId, String fileName, String sourceDeviceId) throws IOException {
        // Create sync directory if it doesn't exist
        Path syncPath = Paths.get(syncDir);
        if (!Files.exists(syncPath)) {
            Files.createDirectories(syncPath);
        }

        // Download file from source device
        String downloadUrl = String.format("http://%s:8081/api/files/%s/download", sourceDeviceId, fileId);
        byte[] fileContent = restTemplate.getForObject(downloadUrl, byte[].class);

        // Save file
        Path filePath = syncPath.resolve(fileName);
        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            fos.write(fileContent);
        }
    }

    private boolean shouldSyncFile(String filePath, String deviceId) {
        List<SyncRule> rules = ruleRepository.findByDeviceId(deviceId);
        return rules.stream().anyMatch(rule -> 
            rule.isEnabled() && 
            filePath.matches(rule.getPathPattern()) &&
            rule.getDirection() != SyncDirection.UPLOAD_ONLY
        );
    }

    private void transferMetadata(String fileId, String deviceId) {
        try {
            // Get file metadata from MongoDB
            Document fileMetadata = mongoTemplate.getCollection("file_metadata")
                .find(com.mongodb.client.model.Filters.eq("fileId", fileId))
                .first();
            
            if (fileMetadata == null) {
                log.error("Metadata not found for file: {}", fileId);
                return;
            }
            
            // Convert to JSON for transfer
            String metadataJson = objectMapper.writeValueAsString(fileMetadata);
            
            // Get device info
            Optional<DeviceInfo> deviceOpt = deviceInfoRepository.findById(deviceId);
            if (!deviceOpt.isPresent()) {
                log.error("Device not found: {}", deviceId);
                return;
            }
            
            DeviceInfo device = deviceOpt.get();
            
            // Send metadata to device
            String metadataUrl = String.format("http://%s:8081/api/files/metadata", device.getDeviceName());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> request = new HttpEntity<>(metadataJson, headers);
            restTemplate.postForEntity(metadataUrl, request, Void.class);
            
            log.info("Metadata transferred for file: {} to device: {}", fileId, deviceId);
        } catch (Exception e) {
            log.error("Error transferring metadata for file: {} to device: {}", fileId, deviceId, e);
        }
    }

    private void transferFileChunks(String fileId, String deviceId) {
        try {
            // Get file from GridFS
            GridFSBucket gridFSBucket = GridFSBuckets.create(mongoTemplate.getDb());
            GridFSFile gridFSFile = gridFSBucket.find(com.mongodb.client.model.Filters.eq("_id", new ObjectId(fileId))).first();
            
            if (gridFSFile == null) {
                log.error("File not found in GridFS: {}", fileId);
                return;
            }
            
            // Get device info
            Optional<DeviceInfo> deviceOpt = deviceInfoRepository.findById(deviceId);
            if (!deviceOpt.isPresent()) {
                log.error("Device not found: {}", deviceId);
                return;
            }
            
            DeviceInfo device = deviceOpt.get();
            
            // Prepare for chunked transfer
            String fileName = gridFSFile.getFilename();
            long fileSize = gridFSFile.getLength();
            int chunkSize = 1024 * 1024; // 1MB chunks
            long totalChunks = (fileSize + chunkSize - 1) / chunkSize;
            
            // Create a temporary file to store the downloaded content
            Path tempFilePath = Files.createTempFile("gridfs-", fileName);
            try (GridFSDownloadStream downloadStream = gridFSBucket.openDownloadStream(gridFSFile.getObjectId());
                 FileOutputStream outputStream = new FileOutputStream(tempFilePath.toFile())) {
                
                byte[] buffer = new byte[chunkSize];
                int bytesRead;
                long currentChunk = 0;
                
                while ((bytesRead = downloadStream.read(buffer)) != -1) {
                    // Before creating ByteArrayResource
                    final long chunkIndexForResource = currentChunk;
                    ByteArrayResource resource = new ByteArrayResource(java.util.Arrays.copyOf(buffer, bytesRead)) {
                        @Override
                        public String getFilename() {
                            return fileName + ".part" + chunkIndexForResource;
                        }
                    };
                    
                    // Send chunk to device
                    String chunkUrl = String.format("http://%s:8081/api/files/chunk", device.getDeviceName());
                    
                    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                    body.add("fileId", fileId);
                    body.add("fileName", fileName);
                    body.add("chunkIndex", currentChunk);
                    body.add("totalChunks", totalChunks);
                    
                    body.add("chunk", resource);
                    
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
                    
                    HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
                    restTemplate.postForEntity(chunkUrl, requestEntity, Void.class);
                    
                    // Write to local temp file as well
                    outputStream.write(buffer, 0, bytesRead);
                    
                    currentChunk++;
                    
                    // Update progress
                    double progress = 0.3 + (0.6 * currentChunk / totalChunks);
                    // replicationTracker.trackProgress(fileId, deviceId, progress);
                }
            }
            
            // Copy the file to the local storage directory
            Path storageDir = Paths.get(syncDir);
            if (!Files.exists(storageDir)) {
                Files.createDirectories(storageDir);
            }
            
            Path targetPath = storageDir.resolve(fileName);
            Files.copy(tempFilePath, targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            
            // Delete the temp file
            Files.delete(tempFilePath);
            
            log.info("File chunks transferred for file: {} to device: {}", fileId, deviceId);
        } catch (Exception e) {
            log.error("Error transferring file chunks for file: {} to device: {}", fileId, deviceId, e);
        }
    }

    private void verifyTransfer(String fileId, String deviceId) {
        try {
            // Get device info
            Optional<DeviceInfo> deviceOpt = deviceInfoRepository.findById(deviceId);
            if (!deviceOpt.isPresent()) {
                log.error("Device not found: {}", deviceId);
                return;
            }
            
            DeviceInfo device = deviceOpt.get();
            
            // Send verification request
            String verifyUrl = String.format("http://%s:8081/api/files/%s/verify", device.getDeviceName(), fileId);
            Boolean verified = restTemplate.getForObject(verifyUrl, Boolean.class);
            
            if (Boolean.TRUE.equals(verified)) {
                log.info("File transfer verified for file: {} to device: {}", fileId, deviceId);
                
                // Update sync history
                SyncHistory syncHistory = new SyncHistory();
                syncHistory.setFileId(fileId);
                syncHistory.setSourceDeviceId(deviceId);
                syncHistory.setTargetDeviceId(deviceId);
                syncHistory.setSyncTime(LocalDateTime.now());
                syncHistory.setStatus("COMPLETED");
                
                mongoTemplate.save(syncHistory, "sync_history");
            } else {
                log.error("File transfer verification failed for file: {} to device: {}", fileId, deviceId);
                throw new RuntimeException("File transfer verification failed");
            }
        } catch (Exception e) {
            log.error("Error verifying file transfer for file: {} to device: {}", fileId, deviceId, e);
        }
    }

    private List<DeviceInfo> getOnlineDevices() {
        // Get all devices that are marked as online
        List<DeviceInfo> onlineDevices = deviceInfoRepository.findByOnline(true);
        
        // Filter out the current device
        return onlineDevices.stream()
            .filter(device -> !device.getId().equals(deviceId))
            .collect(Collectors.toList());
    }
}