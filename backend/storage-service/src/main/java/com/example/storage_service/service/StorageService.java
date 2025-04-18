package com.example.storage_service.service;

import com.example.storage_service.model.DeviceInfo;
import com.example.storage_service.model.FileChunk;
import com.example.storage_service.model.FileTransferPayload;
import com.example.storage_service.repository.DeviceRepository;
import com.mongodb.client.gridfs.model.GridFSFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.util.Pair;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.apache.commons.io.IOUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.google.common.collect.Lists;

import static org.apache.commons.io.IOUtils.toByteArray;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageService {
    private final GridFsTemplate gridFsTemplate;
    private final DeviceRepository deviceRepository;
    private final RestTemplate restTemplate;
    private final MongoTemplate mongoTemplate;

    @Value("${storage.dir}")
    private String storageDir;

    public String storeFile(MultipartFile file, String deviceId) {
        try {
            DeviceInfo device = deviceRepository.findById(deviceId)
                    .orElseThrow(() -> new RuntimeException("Device not found: " + deviceId));

            Path devicePath = Paths.get(storageDir, deviceId);
            Files.createDirectories(devicePath);

            Path filePath = devicePath.resolve(file.getOriginalFilename());
            Files.copy(file.getInputStream(), filePath);

            log.info("File stored successfully: {} for device: {}", filePath, deviceId);
            return filePath.toString();
        } catch (IOException e) {
            log.error("Error storing file for device {}: {}", deviceId, e.getMessage());
            throw new RuntimeException("Failed to store file", e);
        }
    }

    public List<DeviceInfo> getAllDevices() {
        log.info("Retrieving all devices");
        return deviceRepository.findAll();
    }

    public DeviceInfo getDevice(String deviceId) {
        log.info("Retrieving device: {}", deviceId);
        return deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found: " + deviceId));
    }

    public byte[] getFile(String fileId) {
        GridFSFile file = gridFsTemplate.findOne(new Query(Criteria.where("filename").regex(fileId)));
        if (file == null) throw new RuntimeException("File not found");

        try (InputStream inputStream = gridFsTemplate.getResource(file).getInputStream()) {
            return toByteArray(inputStream);
        } catch (IOException e) {
            throw new RuntimeException("Failed to download file", e);
        }
    }

    // --- ADD RETRY LOGIC FOR FILE DOWNLOAD/SYNC ---
    public void syncFileToDevice(String fileId, String deviceId) {
        int maxRetries = 3;
        int attempts = 0;
        while (attempts < maxRetries) {
            try {
                if (isFileReplicated(deviceId, fileId)) {
                    log.info("File {} already replicated for device {}", fileId, deviceId);
                    return;
                }
                byte[] fileData = downloadFile(fileId);
                Path deviceDir = getDeviceDirectory(deviceId);
                Files.createDirectories(deviceDir);
                GridFSFile file = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(new ObjectId(fileId))));
                String filename = file.getFilename();
                String targetDir = file.getMetadata() != null && file.getMetadata().containsKey("targetDirectory")
                        ? file.getMetadata().getString("targetDirectory") : "";
                Path targetPath = deviceDir.resolve(targetDir).resolve(filename);
                Files.createDirectories(targetPath.getParent());
                Files.write(targetPath, fileData);
                markFileReplicated(deviceId, fileId);
                log.info("File {} replicated to device {} at {}", fileId, deviceId, targetPath);
                return;
            } catch (Exception e) {
                attempts++;
                log.error("Attempt {}/{}: Failed to sync file {} to device {}: {}", attempts, maxRetries, fileId, deviceId, e.getMessage());
                if (attempts >= maxRetries) {
                    // Optionally, mark as failed in DB
                    log.error("File {} failed to replicate to device {} after {} attempts", fileId, deviceId, maxRetries);
                } else {
                    try { Thread.sleep(1000 * attempts); } catch (InterruptedException ignored) {}
                }
            }
        }
    }

    public void syncAllFilesToDevice(String deviceId) {
        List<GridFSFile> filesToSync = findFilesToSync(deviceId);
        for (GridFSFile file : filesToSync) {
            syncFileToDevice(file.getObjectId().toString(), deviceId);
        }
    }

    public void syncMultipleFiles(List<String> fileIds) {
        try {
            List<Pair<String, byte[]>> files = fileIds.stream()
                .map(id -> Pair.of(id, getFile(id)))
                .collect(Collectors.toList());
            
            // Process in batches of 10
            Lists.partition(files, 10).forEach(batch -> {
                mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, FileChunk.class)
                    .insert(batch.stream()
                        .map(pair -> new FileChunk(pair.getFirst(), pair.getSecond()))
                        .collect(Collectors.toList())
                    )
                    .execute();
            });
        } catch (Exception e) {
            log.error("Bulk sync failed: {}", e.getMessage());
            throw new RuntimeException("Bulk operation failed", e);
        }
    }

    // --- BACKGROUND SYNC LOGIC FOR FILE REPLICATION ---
    @Scheduled(fixedRate = 30000) // every 30 seconds
    public void syncFilesFromAtlas() {
        List<DeviceInfo> devices = deviceRepository.findAll();
        for (DeviceInfo device : devices) {
            if (!device.isOffline()) {
                syncAllFilesToDevice(device.getId());
            }
        }
    }

    // --- FILE REPLICATION STATE TRACKING ---
    private boolean isFileReplicated(String deviceId, String fileId) {
        // Query MongoDB for replication state (implement actual logic)
        // Example: Use a collection 'replication_status' with (deviceId, fileId)
        Query query = new Query(Criteria.where("deviceId").is(deviceId).and("fileId").is(fileId));
        return mongoTemplate.exists(query, "replication_status");
    }

    private void markFileReplicated(String deviceId, String fileId) {
        // Insert replication state into MongoDB
        Document doc = new Document("deviceId", deviceId)
                .append("fileId", fileId)
                .append("timestamp", System.currentTimeMillis());
        mongoTemplate.insert(doc, "replication_status");
    }

    private List<GridFSFile> findFilesToSync(String deviceId) {
        // Get all files in GridFS
        List<GridFSFile> allFiles = new ArrayList<>();
        gridFsTemplate.find(new Query()).forEach(allFiles::add);
        // Filter out files already replicated for this device
        List<GridFSFile> filesToSync = new ArrayList<>();
        for (GridFSFile file : allFiles) {
            if (!isFileReplicated(deviceId, file.getObjectId().toString())) {
                filesToSync.add(file);
            }
        }
        return filesToSync;
    }

    private byte[] downloadFile(String fileId) {
        GridFSFile file = gridFsTemplate.findOne(new Query(Criteria.where("filename").regex(fileId)));
        if (file == null) throw new RuntimeException("File not found");

        try (InputStream inputStream = gridFsTemplate.getResource(file).getInputStream()) {
            return toByteArray(inputStream);
        } catch (IOException e) {
            throw new RuntimeException("Failed to download file", e);
        }
    }

    private Path getDeviceDirectory(String deviceId) {
        return Paths.get(storageDir, deviceId);
    }
}