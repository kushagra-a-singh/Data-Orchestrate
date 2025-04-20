package com.example.storage_service.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import com.example.storage_service.model.ReplicationRequest;

@RestController
@RequestMapping("/replicate")
public class ReplicationController {

    @Value("${file.storage-dir}")
    private String STORAGE_DIR;

    @PostMapping
    public String receiveFileReplicationRequest(@RequestBody ReplicationRequest request) {
        // Download file from source device using HTTP
        try {
            String sourceDeviceUrl = request.getSourceDeviceUrl();
            String fileId = request.getFileId();
            String fileName = request.getFileName();
            // Download file from source device
            URL url = new URL(sourceDeviceUrl + "/api/files/download/" + fileId);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (InputStream is = conn.getInputStream()) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
            }
            Files.write(Paths.get(STORAGE_DIR + fileName), baos.toByteArray());
            return "File replicated successfully!";
        } catch (Exception e) {
            return "Replication failed: " + e.getMessage();
        }
    }
}
