package com.dataorchestrate.fileupload;

import com.dataorchestrate.common.DeviceIdentifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class FileUploadService {
    private static final Logger logger = LoggerFactory.getLogger(FileUploadService.class);
    private final String uploadDir;
    private final DeviceIdentifier deviceIdentifier;
    
    @Autowired
    public FileUploadService(
            @Value("${file.upload.directory}") String uploadDir,
            DeviceIdentifier deviceIdentifier) {
        this.uploadDir = uploadDir;
        this.deviceIdentifier = deviceIdentifier;
        logger.info("Initializing FileUploadService for device: {}", deviceIdentifier.getDeviceId());
        createUploadDirectory();
    }
    
    public String uploadFile(MultipartFile file, String uploadedBy) throws IOException {
        String deviceId = deviceIdentifier.getDeviceId();
        String fileName = file.getOriginalFilename();
        String fileId = UUID.randomUUID().toString();
        
        logger.info("Uploading file {} from device {} by user {}", fileName, deviceId, uploadedBy);
        
        // Create device-specific directory
        Path deviceDir = Paths.get(uploadDir, deviceId);
        Files.createDirectories(deviceDir);
        
        // Save file
        Path filePath = deviceDir.resolve(fileId + "_" + fileName);
        file.transferTo(filePath.toFile());
        
        logger.info("File {} uploaded successfully to {}", fileName, filePath);
        return fileId;
    }
    
    private void createUploadDirectory() {
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
            logger.info("Created upload directory: {}", uploadDir);
        }
    }
} 