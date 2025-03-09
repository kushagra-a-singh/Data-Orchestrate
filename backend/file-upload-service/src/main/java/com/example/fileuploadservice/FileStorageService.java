package com.mpjmp.fileupload.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final String UPLOAD_DIR = "C:/mpj-mp/uploads/";

    public String storeFile(MultipartFile file) {
        String fileId = UUID.randomUUID().toString();
        String filePath = UPLOAD_DIR + fileId + "-" + file.getOriginalFilename();

        try {
            File destFile = new File(filePath);
            file.transferTo(destFile);
            return fileId;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }
}
