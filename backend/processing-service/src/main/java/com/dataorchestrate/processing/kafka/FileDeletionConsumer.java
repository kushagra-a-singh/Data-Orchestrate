package com.dataorchestrate.processing.kafka;

import com.dataorchestrate.processing.model.ProcessingJob;
import com.dataorchestrate.processing.repository.ProcessingJobRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class FileDeletionConsumer {

    private final ProcessingJobRepository processingJobRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public FileDeletionConsumer(ProcessingJobRepository processingJobRepository, ObjectMapper objectMapper) {
        this.processingJobRepository = processingJobRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
        topics = "${app.kafka.topics.file-deletion}",
        groupId = "${app.kafka.groups.file-deletion}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleFileDeletion(
        String message,
        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
        @Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition,
        @Header(KafkaHeaders.OFFSET) Long offset
    ) throws JsonProcessingException, IOException {
        try {
            log.info("Received file deletion request from topic: {}, partition: {}, offset: {}, message: {}", 
                topic, partition, offset, message);
            
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            String fileId = (String) event.get("fileId");
            String fileName = (String) event.get("fileName");
            
            // Delete the file from processed directory
            Path processedFilePath = Paths.get("processed_" + fileName);
            if (Files.exists(processedFilePath)) {
                Files.delete(processedFilePath);
                log.info("Deleted processed file: {}", fileName);
            }
            
            // Delete the file from upload directory
            Path uploadFilePath = Paths.get(fileName);
            if (Files.exists(uploadFilePath)) {
                Files.delete(uploadFilePath);
                log.info("Deleted uploaded file: {}", fileName);
            }
            
            // Update processing job status
            Optional<ProcessingJob> jobOptional = processingJobRepository.findByFileId(fileId);
            jobOptional.ifPresent(job -> {
                job.setStatus("DELETED");
                job.setCompletedAt(java.time.LocalDateTime.now());
                processingJobRepository.save(job);
                log.info("Successfully processed file deletion for file: {}", fileId);
            });
            
            if (!jobOptional.isPresent()) {
                log.error("Processing job not found for file: {}", fileId);
            }
        } catch (Exception e) {
            log.error("Error handling file deletion", e);
            throw e; // Rethrow to trigger retry
        }
    }
} 