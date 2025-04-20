package com.mpjmp.fileupload.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class FileStorageService {

    // --- METADATA VALIDATION ---
    private void validateMetadata(MultipartFile file, String uploadedBy, String targetDirectory) {
        if (uploadedBy == null || uploadedBy.isBlank()) throw new IllegalArgumentException("Uploader must be specified");
        if (file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()) throw new IllegalArgumentException("Filename required");
        if (file.getContentType() == null || file.getContentType().isBlank()) throw new IllegalArgumentException("File type required");
        if (targetDirectory == null) throw new IllegalArgumentException("Target directory required");
    }

    // --- ENSURE FULL METADATA ON UPLOAD ---
    public String storeFile(MultipartFile file, String uploadedBy, String targetDirectory) throws IOException {
        validateMetadata(file, uploadedBy, targetDirectory);
        // Define the base uploads directory (adjust as needed)
        Path uploadsDir = Paths.get("./data/uploads");
        Files.createDirectories(uploadsDir);
        Path targetDir = uploadsDir.resolve(targetDirectory);
        Files.createDirectories(targetDir);
        Path filePath = targetDir.resolve(file.getOriginalFilename());
        file.transferTo(filePath);
        return filePath.getFileName().toString(); // Return file name for downstream use
    }
}
