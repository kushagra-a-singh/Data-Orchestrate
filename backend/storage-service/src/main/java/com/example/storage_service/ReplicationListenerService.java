package com.mpjmp.storage.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;

@Service
public class ReplicationListenerService {
    
    private final RestTemplate restTemplate = new RestTemplate();

    @KafkaListener(topics = "file-replication-topic", groupId = "storage-group")
    public void receiveFileReplicationRequest(String message) {
        try {
            String[] parts = message.split(",");
            String fileName = parts[0];
            String uploaderIp = parts[1];

            System.out.println("Received replication request for " + fileName + " from " + uploaderIp);

            InputStream inputStream = new URL("http://" + uploaderIp + "/download/" + fileName).openStream();
            File file = new File("storage/" + fileName);
            FileOutputStream outputStream = new FileOutputStream(file);
            inputStream.transferTo(outputStream);

            System.out.println("File replicated successfully: " + fileName);
        } catch (Exception e) {
            System.err.println("Error replicating file: " + e.getMessage());
        }
    }
}
