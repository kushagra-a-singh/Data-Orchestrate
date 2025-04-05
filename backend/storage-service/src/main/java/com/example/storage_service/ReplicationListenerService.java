package com.example.storage_service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.io.IOException;
import java.util.List;
import com.example.storage_service.model.FileMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.storage_service.repository.FileMetadataRepository;
import javax.annotation.PostConstruct;

@Service
public class ReplicationListenerService {
    
    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    @KafkaListener(topics = "file-replication-topic", groupId = "storage-group")
    public void receiveFileReplicationRequest(String message) {
        try {
            String[] parts = message.split(",");
            String fileName = parts[0];
            String uploaderIp = parts[1];

            fetchFileFromSource(fileName, uploaderIp);

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

    private void fetchFileFromSource(String fileName, String uploaderIp) throws IOException {
        InputStream inputStream = new URL("http://" + uploaderIp + "/download/" + fileName).openStream();
        File file = new File("storage/" + fileName);
        FileOutputStream outputStream = new FileOutputStream(file);
        inputStream.transferTo(outputStream);
        inputStream.close();
        outputStream.close();
    }

    public void syncMissingFiles() {
        List<FileMetadata> files = fileMetadataRepository.findAll();
        for (FileMetadata file : files) {
            try {
                fetchFileFromSource(file.getFileName(), file.getUploaderIp());
            } catch (IOException e) {
                System.err.println("Error syncing file: " + e.getMessage());
            }
        }
    }

    @PostConstruct
    public void init() {
        syncMissingFiles();
    }
}
