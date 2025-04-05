package com.example.processing_service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import com.example.common.repository.FileMetadataRepository;
import com.example.common.model.FileMetadata;

@Service
public class FileProcessingConsumer {

    @Autowired
    private ProcessingService processingService;

    @Autowired
    private FileMetadataRepository metadataRepository;

    @KafkaListener(
        topics = "file-processing-topic",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void processFile(String fileId, String fileContent) {
        try {
            // Determine file type and process accordingly
            FileMetadata metadata = metadataRepository.findById(fileId).orElseThrow();
            
            if (metadata.getFileType().equals("application/pdf")) {
                processingService.extractTextFromPdf(fileId, fileContent);
            } else {
                processingService.compressFile(fileId, fileContent);
            }
        } catch (Exception e) {
            System.err.println("Error processing file: " + e.getMessage());
        }

    }
}
