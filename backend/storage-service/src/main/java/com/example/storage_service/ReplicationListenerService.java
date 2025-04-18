package com.mpjmp.storage.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.RestTemplate;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;

@Service
public class ReplicationListenerService {
    
    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/replicate-file")
    public ResponseEntity<String> receiveFileReplicationRequest(@RequestBody ReplicationRequest request) {
        try {
            String fileName = request.getFileName();
            String uploaderIp = request.getUploaderIp();

            System.out.println("Received replication request for " + fileName + " from " + uploaderIp);

            InputStream inputStream = new URL("http://" + uploaderIp + "/download/" + fileName).openStream();
            File file = new File("storage/" + fileName);
            FileOutputStream outputStream = new FileOutputStream(file);
            inputStream.transferTo(outputStream);

            System.out.println("File replicated successfully: " + fileName);
            return ResponseEntity.ok("File replicated successfully");
        } catch (Exception e) {
            System.err.println("Error replicating file: " + e.getMessage());
            return ResponseEntity.badRequest().body("Error replicating file: " + e.getMessage());
        }
    }
}

class ReplicationRequest {
    private String fileName;
    private String uploaderIp;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getUploaderIp() {
        return uploaderIp;
    }

    public void setUploaderIp(String uploaderIp) {
        this.uploaderIp = uploaderIp;
    }
}
