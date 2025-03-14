package com.mpjmp.storage.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class StorageService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public StorageService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void storeFile(byte[] fileData, String fileName, String uploaderIp) throws IOException {
        String filePath = Paths.get("storage/", fileName).toString();
        Files.write(Paths.get(filePath), fileData);

        kafkaTemplate.send("file-replication-topic", fileName + "," + uploaderIp);
    }
}
