package com.mpjmp.processing.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mpjmp.processing.service.FileProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileDeletionConsumer {

    private final FileProcessingService fileProcessingService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics = "${kafka.topic.file-deletion}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 5000)
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
            Path processedFilePath = Paths.get(fileProcessingService.getProcessedDir(), "processed_" + fileName);
            if (Files.exists(processedFilePath)) {
                Files.delete(processedFilePath);
                log.info("Deleted processed file: {}", fileName);
            }
            
            // Delete the file from upload directory
            Path uploadFilePath = Paths.get(fileProcessingService.getUploadDir(), fileName);
            if (Files.exists(uploadFilePath)) {
                Files.delete(uploadFilePath);
                log.info("Deleted uploaded file: {}", fileName);
            }
            
            // Update processing job status
            fileProcessingService.markJobAsDeleted(fileId);
            
        } catch (Exception e) {
            log.error("Error handling file deletion", e);
            throw e; // Rethrow to trigger retry
        }
    }
} 