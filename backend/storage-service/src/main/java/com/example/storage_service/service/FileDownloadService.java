package com.example.storage_service.service;

import com.mongodb.client.gridfs.GridFSBucket;
import org.bson.types.ObjectId;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.example.storage_service.events.FileChangeEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileDownloadService {
    private final GridFSBucket gridFsBucket;
    private final Path baseDirectory;
    private final DeviceRegistryService deviceRegistry;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FileDownloadService.class);
    
    public Path getDeviceDirectory(String deviceId) {
        return baseDirectory.resolve(deviceId);
    }
    
    @KafkaListener(topics = "file-changes")
    public void handleFileChange(FileChangeEvent event) {
        event.deviceIds().forEach(deviceId -> {
            try {
                Path deviceDir = getDeviceDirectory(deviceId);
                Files.createDirectories(deviceDir);
                
                byte[] fileData = downloadFile(event.fileId());
                Path targetPath = deviceDir.resolve(event.filename());
                
                Files.write(targetPath, fileData);
            } catch (IOException e) {
                log.error("Failed to sync to device {}: {}", deviceId, e.getMessage());
            }
        });
    }
    
    private byte[] downloadFile(String fileId) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        gridFsBucket.downloadToStream(new ObjectId(fileId), outputStream);
        return outputStream.toByteArray();
    }
}
