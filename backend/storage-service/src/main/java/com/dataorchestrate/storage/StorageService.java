package com.dataorchestrate.storage;

import com.dataorchestrate.common.DeviceIdentifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class StorageService {
    private static final Logger logger = LoggerFactory.getLogger(StorageService.class);
    private final String storageDir;
    private final DeviceIdentifier deviceIdentifier;
    
    @Autowired
    public StorageService(
            @Value("${file.storage.directory}") String storageDir,
            DeviceIdentifier deviceIdentifier) {
        this.storageDir = storageDir;
        this.deviceIdentifier = deviceIdentifier;
        logger.info("Initializing StorageService for device: {}", deviceIdentifier.getDeviceId());
        createStorageDirectory();
    }
    
    public void storeFile(byte[] content, String fileId, String fileName) throws IOException {
        String deviceId = deviceIdentifier.getDeviceId();
        logger.info("Storing file {} for device {}", fileName, deviceId);
        
        // Create device-specific directory
        Path deviceDir = Paths.get(storageDir, deviceId);
        Files.createDirectories(deviceDir);
        
        // Save file with proper naming
        Path filePath = deviceDir.resolve(fileId + "_" + fileName);
        Files.write(filePath, content);
        
        // Also save a copy to the data directory for processing
        Path dataDir = Paths.get("./data/storage");
        Files.createDirectories(dataDir);
        Path dataFilePath = dataDir.resolve(fileId + "_" + fileName);
        Files.write(dataFilePath, content);
        
        logger.info("File {} stored successfully at {} and in data directory", fileName, filePath);
    }
    
    public byte[] retrieveFile(String fileId, String fileName) throws IOException {
        String deviceId = deviceIdentifier.getDeviceId();
        logger.info("Retrieving file {} for device {}", fileName, deviceId);
        
        Path filePath = Paths.get(storageDir, deviceId, fileId + "_" + fileName);
        if (!Files.exists(filePath)) {
            logger.warn("File {} not found for device {}", fileName, deviceId);
            return null;
        }
        
        byte[] content = Files.readAllBytes(filePath);
        logger.info("File {} retrieved successfully", fileName);
        return content;
    }
    
    private void createStorageDirectory() {
        File directory = new File(storageDir);
        if (!directory.exists()) {
            directory.mkdirs();
            logger.info("Created storage directory: {}", storageDir);
        }
    }
}