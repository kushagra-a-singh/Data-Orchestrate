package com.mpjmp.fileupload.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload-dir}")
    private String UPLOAD_DIR;

    public String storeFile(MultipartFile file) {
        String fileId = UUID.randomUUID().toString();
        String basePath = Paths.get("").toAbsolutePath().toString();
        String filePath = Paths.get(basePath, UPLOAD_DIR, fileId + "-" + file.getOriginalFilename()).toString();

        try {
            Files.createDirectories(Paths.get(basePath, UPLOAD_DIR));
            file.transferTo(new File(filePath));
            return fileId;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }
}
