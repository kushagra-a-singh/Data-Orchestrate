package com.dataorchestrate.processing.service;

import com.dataorchestrate.processing.model.ProcessingJob;
import com.dataorchestrate.processing.repository.ProcessingJobRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bson.Document;

@Service
@Slf4j
public class FileProcessingService {

    private final ProcessingJobRepository processingJobRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final MongoTemplate mongoTemplate;

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${app.processed.dir}")
    private String processedDir;

    @Value("${kafka.topic.file-status}")
    private String fileStatusTopic;

    @Value("${kafka.topic.notifications}")
    private String notificationsTopic;

    @Value("${app.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${app.retry.delay:5000}")
    private long retryDelay;

    @Autowired
    public FileProcessingService(ProcessingJobRepository processingJobRepository, KafkaTemplate<String, String> kafkaTemplate, com.fasterxml.jackson.databind.ObjectMapper objectMapper, MongoTemplate mongoTemplate) {
        this.processingJobRepository = processingJobRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.mongoTemplate = mongoTemplate;
    }

    public void processFile(String message) {
        try {
            Map<String, Object> event = parseEvent(message);
            String fileId = (String) event.get("fileId");
            String fileName = (String) event.get("fileName");
            String originalFileName = (String) event.get("originalFileName");
            String status = (String) event.get("status");
            String errorMessage = (String) event.get("errorMessage");
            LocalDateTime startedAt = (LocalDateTime) event.get("startedAt");
            LocalDateTime completedAt = (LocalDateTime) event.get("completedAt");
            Map<String, Object> processingResult = (Map<String, Object>) event.get("processingResult");
            Map<String, Object> metadata = (Map<String, Object>) event.get("metadata");

            ProcessingJob job = new ProcessingJob();
            job.setId(UUID.randomUUID().toString());
            job.setFileId(fileId);
            job.setFileName(fileName);
            job.setOriginalFileName(originalFileName);
            job.setStatus(status);
            job.setStartedAt(startedAt);
            job.setMetadata(metadata);

            processingJobRepository.save(job);
        } catch (Exception e) {
            log.error("Error processing file: {}", message, e);
            throw e;
        }
    }

    public void updateJobStatus(String fileId, String status) {
        Optional<ProcessingJob> jobOptional = processingJobRepository.findByFileId(fileId);
        if (jobOptional.isPresent()) {
            ProcessingJob job = jobOptional.get();
            job.setStatus(status);
            processingJobRepository.save(job);
        }
    }

    public void updateJobError(String fileId, String errorMessage) {
        Optional<ProcessingJob> jobOptional = processingJobRepository.findByFileId(fileId);
        if (jobOptional.isPresent()) {
            ProcessingJob job = jobOptional.get();
            job.setStatus("FAILED");
            job.setErrorMessage(errorMessage);
            job.setCompletedAt(LocalDateTime.now());
            processingJobRepository.save(job);
        }
    }

    public void updateJobSuccess(String fileId, Map<String, Object> processingResult) {
        Optional<ProcessingJob> jobOptional = processingJobRepository.findByFileId(fileId);
        if (jobOptional.isPresent()) {
            ProcessingJob job = jobOptional.get();
            job.setStatus("COMPLETED");
            job.setProcessingResult(processingResult);
            job.setCompletedAt(LocalDateTime.now());
            processingJobRepository.save(job);
        }
    }

    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 5000)
    )
    private void processFileWithRetry(String fileId, String fileName, String originalFileName, ProcessingJob job) throws Exception {
        try {
            // Get the file from storage
            String storagePath = processingJobRepository.findByFileId(fileId)
                    .map(j -> (String) j.getMetadata().get("storagePath"))
                    .orElseThrow(() -> new RuntimeException("Storage path not found for file: " + fileId));

            Path sourcePath = Paths.get(storagePath);
            Path targetPath = Paths.get(processedDir, "processed_" + fileName);

            // Create processed directory if it doesn't exist
            Files.createDirectories(Paths.get(processedDir));

            // Process the file
            processFileContent(sourcePath, targetPath);

            // Update job with success
            updateJobSuccess(fileId, createProcessingResult(sourcePath, targetPath));

            // Update file status
            updateFileStatus(fileId, "COMPLETED", null);

            // Send success notification
            sendNotification("SUCCESS", "File processed successfully: " + originalFileName);

        } catch (Exception e) {
            log.error("Error in processing attempt", e);
            updateJobError(fileId, "Retry attempt: " + e.getMessage());
            throw e; // Rethrow to trigger retry
        }
    }

    private void handleProcessingError(String message, String errorMessage) {
        try {
            Map<String, Object> event = parseEvent(message);
            String fileId = (String) event.get("fileId");
            String originalFileName = (String) event.get("originalFileName");

            updateJobError(fileId, errorMessage);

            // Update file status
            updateFileStatus(fileId, "FAILED", errorMessage);

            // Send error notification
            sendNotification("ERROR", "Failed to process file: " + originalFileName + " - " + errorMessage);

        } catch (Exception e) {
            log.error("Error handling processing error", e);
            throw e;
        }
    }

    private Map<String, Object> createProcessingResult(Path sourcePath, Path targetPath) {
        Map<String, Object> result = new HashMap<>();
        try {
            result.put("sourceSize", Files.size(sourcePath));
            result.put("targetSize", Files.size(targetPath));
            result.put("processedAt", LocalDateTime.now().toString());
        } catch (Exception e) {
            log.error("Error creating processing result", e);
        }
        return result;
    }

    private void updateFileStatus(String fileId, String status, String errorMessage) {
        try {
            Map<String, Object> statusEvent = new HashMap<>();
            statusEvent.put("fileId", fileId);
            statusEvent.put("newStatus", status);
            statusEvent.put("errorMessage", errorMessage);
            statusEvent.put("updatedAt", LocalDateTime.now().toString());

            kafkaTemplate.send(fileStatusTopic, objectMapper.writeValueAsString(statusEvent));
        } catch (Exception e) {
            log.error("Error updating file status", e);
        }
    }

    private void sendNotification(String type, String message) {
        try {
            Map<String, String> notification = new HashMap<>();
            notification.put("type", type);
            notification.put("message", message);
            kafkaTemplate.send(notificationsTopic, objectMapper.writeValueAsString(notification));
        } catch (Exception e) {
            log.error("Failed to send notification", e);
        }
    }

    private void processFileContent(Path sourcePath, Path targetPath) throws Exception {
        // Implementation for processing file content
        Files.copy(sourcePath, targetPath);
    }

    public List<ProcessingJob> getJobsByFileId(String fileId) {
        Optional<ProcessingJob> jobOptional = processingJobRepository.findByFileId(fileId);
        return jobOptional.map(job -> List.of(job))
                .orElse(List.of());
    }

    public List<ProcessingJob> getJobsByStatus(String status) {
        return processingJobRepository.findByStatus(status);
    }

    public List<ProcessingJob> getJobsByStatusAndAge(String status, LocalDateTime dateTime) {
        return processingJobRepository.findByStatusAndStartedAtBefore(status, dateTime);
    }

    public String getProcessedDir() {
        return processedDir;
    }

    public String getUploadDir() {
        return uploadDir;
    }

    public void markJobAsDeleted(String fileId) {
        Optional<ProcessingJob> jobOptional = processingJobRepository.findByFileId(fileId);
        jobOptional.ifPresent(job -> {
            job.setStatus("DELETED");
            job.setCompletedAt(LocalDateTime.now());
            processingJobRepository.save(job);
        });
    }

    private Map<String, Object> parseEvent(String message) {
        try {
            return objectMapper.readValue(message, new com.fasterxml.jackson.core.type.TypeReference<HashMap<String, Object>>() {});
        } catch (Exception e) {
            log.error("Error parsing event", e);
            throw new RuntimeException(e);
        }
    }

    // Add a PDF text extraction utility method
    private String extractText(File file) throws Exception {
        try (PDDocument document = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    public void processPDF(File file) throws ProcessingException {
        try {
            String extractedText = extractText(file);
            
            Document metadata = new Document()
                .append("originalFile", file.getName())
                .append("extractedText", extractedText)
                .append("processingTime", Instant.now())
                .append("fileSize", file.length());
                
            mongoTemplate.insert(metadata, "extracted_texts");
            log.info("Stored extracted text for: {}", file.getName());
        } catch (Exception e) {
            log.error("PDF processing failed for: {}", file.getName(), e);
            throw new ProcessingException("PDF text extraction failed", e);
        }
    }
}