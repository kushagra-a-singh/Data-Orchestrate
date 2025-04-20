package com.dataorchestrate.processing.service;

import com.dataorchestrate.processing.model.ProcessingJob;
import com.dataorchestrate.processing.model.FileMetadata;
import com.dataorchestrate.processing.repository.ProcessingJobRepository;
import com.dataorchestrate.common.NotificationSender;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
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
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final MongoTemplate mongoTemplate;

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${app.processed.dir}")
    private String processedDir;

    @Autowired
    public FileProcessingService(ProcessingJobRepository processingJobRepository, com.fasterxml.jackson.databind.ObjectMapper objectMapper, MongoTemplate mongoTemplate) {
        this.processingJobRepository = processingJobRepository;
        this.objectMapper = objectMapper;
        this.mongoTemplate = mongoTemplate;
    }

    // Main entry for processing files (extended for PDF extraction)
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

            // Save job metadata
            processingJobRepository.save(job);

            // --- PDF Extraction Integration ---
            if (fileName != null && fileName.toLowerCase().endsWith(".pdf")) {
                // Find the file path (assuming stored in uploadDir/fileId_fileName)
                Path pdfPath = Paths.get(uploadDir, fileId + "_" + fileName);
                File pdfFile = pdfPath.toFile();
                if (pdfFile.exists()) {
                    log.info("Detected PDF file. Extracting text for: {}", pdfFile.getAbsolutePath());
                    processPdfAndUpdateMetadata(pdfFile, fileId);
                } else {
                    log.warn("PDF file not found for extraction: {}", pdfPath);
                }
            }
        } catch (Exception e) {
            log.error("Error processing file: {}", message, e);
            throw new RuntimeException("File processing failed", e);
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

            // TODO: Implement HTTP-based event notifications or calls
            // Send success notification
            // sendNotification("SUCCESS", "File processed successfully: " + originalFileName);

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

            // TODO: Implement HTTP-based event notifications or calls
            // Send error notification
            // sendNotification("ERROR", "Failed to process file: " + originalFileName + " - " + errorMessage);

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

    // Unified PDF processing method: extract text and update FileMetadata
    public void processPdfAndUpdateMetadata(File file, String fileId) throws Exception {
        Optional<FileMetadata> metadataOpt = findFileMetadataByFileId(fileId);
        if (metadataOpt.isEmpty()) {
            log.error("No FileMetadata found for fileId: {}. Cannot save extracted text.", fileId);
            NotificationSender.sendNotification(
                "error",
                "PDF Extraction Failed",
                "No FileMetadata found for fileId: " + fileId,
                null,
                fileId,
                null,
                file.getAbsolutePath()
            );
            return;
        }
        FileMetadata metadata = metadataOpt.get();
        try (PDDocument document = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            metadata.setExtractedText(text);
            if (text != null && !text.isEmpty()) {
                log.info("PDF extracted text (first 100 chars): {}", text.substring(0, Math.min(100, text.length())));
                NotificationSender.sendNotification(
                    "success",
                    "PDF Text Extraction Complete",
                    "Extracted text from PDF: " + file.getName(),
                    1.0,
                    fileId,
                    metadata.getDeviceId(),
                    file.getAbsolutePath()
                );
            } else {
                log.warn("No text extracted from PDF: {}", file.getAbsolutePath());
                NotificationSender.sendNotification(
                    "warning",
                    "PDF Extraction Warning",
                    "No text could be extracted from PDF: " + file.getName(),
                    null,
                    fileId,
                    metadata.getDeviceId(),
                    file.getAbsolutePath()
                );
            }
            // Store additional metadata
            Map<String, Object> additionalMetadata = new HashMap<>();
            additionalMetadata.put("pageCount", document.getNumberOfPages());
            metadata.setMetadata(additionalMetadata);
            // Persist metadata after extraction
            mongoTemplate.save(metadata);
            log.info("Saved FileMetadata with extractedText for file: {}", file.getAbsolutePath());
        } catch (Exception e) {
            log.error("PDF processing failed for: {}", file.getName(), e);
            NotificationSender.sendNotification(
                "error",
                "PDF Extraction Error",
                "Exception during PDF text extraction: " + e.getMessage(),
                null,
                fileId,
                metadataOpt.map(FileMetadata::getDeviceId).orElse(null),
                file.getAbsolutePath()
            );
            throw new Exception("PDF text extraction failed", e);
        }
    }

    // Utility to find FileMetadata by fileId
    private Optional<FileMetadata> findFileMetadataByFileId(String fileId) {
        Query query = new Query(Criteria.where("fileId").is(fileId));
        FileMetadata metadata = mongoTemplate.findOne(query, FileMetadata.class);
        return Optional.ofNullable(metadata);
    }

    // DEPRECATED: Remove old processPDF method that saves to extracted_texts
    // public void processPDF(File file) throws ProcessingException {
    //     ... (removed)
    // }
}