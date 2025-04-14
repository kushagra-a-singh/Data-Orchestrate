package com.mpjmp.processing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mpjmp.processing.model.ProcessingJob;
import com.mpjmp.processing.repository.ProcessingJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileProcessingService {

    private final ProcessingJobRepository processingJobRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

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

    public void processFile(String message) {
        try {
            Map<String, Object> event = objectMapper.readValue(message, new com.fasterxml.jackson.core.type.TypeReference<HashMap<String, Object>>() {});
            String fileId = (String) event.get("fileId");
            String fileName = (String) event.get("fileName");
            String originalFileName = (String) event.get("originalFileName");

            // Create processing job
            ProcessingJob job = new ProcessingJob();
            job.setId(UUID.randomUUID().toString());
            job.setFileId(fileId);
            job.setFileName(fileName);
            job.setOriginalFileName(originalFileName);
            job.setStatus("PROCESSING");
            job.setStartedAt(LocalDateTime.now());
            job.setMetadata(event);

            processingJobRepository.save(job);

            // Update file status
            updateFileStatus(fileId, "PROCESSING", null);

            // Process the file with retry
            processFileWithRetry(fileId, fileName, originalFileName, job);

        } catch (Exception e) {
            log.error("Error processing file", e);
            handleProcessingError(message, e.getMessage());
        }
    }

    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 5000)
    )
    private void processFileWithRetry(String fileId, String fileName, String originalFileName, ProcessingJob job) throws Exception {
        try {
            Path sourcePath = Paths.get(uploadDir, fileName);
            Path targetPath = Paths.get(processedDir, "processed_" + fileName);

            // Create processed directory if it doesn't exist
            Files.createDirectories(Paths.get(processedDir));

            // Process the file (example: copy to processed directory)
            Files.copy(sourcePath, targetPath);

            // Update job with success
            job.setStatus("COMPLETED");
            job.setCompletedAt(LocalDateTime.now());
            job.setProcessingResult(createProcessingResult(sourcePath, targetPath));
            processingJobRepository.save(job);

            // Update file status
            updateFileStatus(fileId, "COMPLETED", null);

            // Send success notification
            sendNotification("SUCCESS", "File processed successfully: " + originalFileName);

        } catch (Exception e) {
            log.error("Error in processing attempt", e);
            job.setStatus("RETRYING");
            job.setErrorMessage("Retry attempt: " + e.getMessage());
            processingJobRepository.save(job);
            throw e; // Rethrow to trigger retry
        }
    }

    private void handleProcessingError(String message, String errorMessage) {
        try {
            Map<String, Object> event = objectMapper.readValue(message, new com.fasterxml.jackson.core.type.TypeReference<HashMap<String, Object>>() {});
            String fileId = (String) event.get("fileId");
            String originalFileName = (String) event.get("originalFileName");

            // Update job with error
            ProcessingJob job = new ProcessingJob();
            job.setId(UUID.randomUUID().toString());
            job.setFileId(fileId);
            job.setFileName((String) event.get("fileName"));
            job.setOriginalFileName(originalFileName);
            job.setStatus("FAILED");
            job.setErrorMessage(errorMessage);
            job.setStartedAt(LocalDateTime.now());
            job.setCompletedAt(LocalDateTime.now());
            job.setMetadata(event);

            processingJobRepository.save(job);

            // Update file status
            updateFileStatus(fileId, "FAILED", errorMessage);

            // Send error notification
            sendNotification("ERROR", "Failed to process file: " + originalFileName + " - " + errorMessage);

        } catch (Exception e) {
            log.error("Error handling processing error", e);
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

    public List<ProcessingJob> getJobsByFileId(String fileId) {
        return processingJobRepository.findByFileId(fileId);
    }

    public List<ProcessingJob> getJobsByStatus(String status) {
        return processingJobRepository.findByStatus(status);
    }

    public List<ProcessingJob> getStuckJobs(String status, int hours) {
        return processingJobRepository.findByStatusAndStartedAtBefore(
            status, 
            LocalDateTime.now().minusHours(hours)
        );
    }

    public String getProcessedDir() {
        return processedDir;
    }

    public String getUploadDir() {
        return uploadDir;
    }

    public void markJobAsDeleted(String fileId) {
        List<ProcessingJob> jobs = processingJobRepository.findByFileId(fileId);
        for (ProcessingJob job : jobs) {
            job.setStatus("DELETED");
            job.setCompletedAt(LocalDateTime.now());
            processingJobRepository.save(job);
        }
    }
} 