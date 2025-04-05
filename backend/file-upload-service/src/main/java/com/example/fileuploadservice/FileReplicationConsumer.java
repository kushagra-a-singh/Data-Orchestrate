package com.example.fileuploadservice;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import java.io.IOException;
import java.nio.file.Path;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Backoff;

@Service
public class FileReplicationConsumer {

    @Value("${UPLOAD_DIR}")
    private String uploadDir;

    @KafkaListener(
        topics = "file-replication-topic",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    @Retryable(value = { IOException.class }, maxAttempts = 5, backoff = @Backoff(delay = 2000))
    public void receiveFile(String fileId, String fileContent) throws IOException {
        Path path = Paths.get(uploadDir + fileId);
        Files.write(path, fileContent.getBytes());
    }

    @Recover
    public void recover(IOException e, String fileId, String fileContent) {
        System.err.println("Failed to write file after retries: " + e.getMessage());
        // Handle recovery logic, e.g., log the error or notify the user
    }
} 