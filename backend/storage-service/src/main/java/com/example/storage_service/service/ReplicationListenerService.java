package com.example.storage_service.service;

import com.example.storage_service.model.DeviceInfo;
import com.example.storage_service.model.ReplicationRequest;
import com.example.storage_service.repository.DeviceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ReplicationListenerService {

    private final StorageService storageService;
    private final DeviceRepository deviceRepository;

    // --- REMOVE KAFKA CONSUMER LOGIC, USE HTTP INSTEAD ---
    // Add HTTP endpoint for file replication
    // Old KafkaListener logic removed as part of Kafka dependency cleanup
    @PostMapping("/replicate-file")
    public ResponseEntity<String> receiveFileReplicationRequest(@RequestBody ReplicationRequest request) {
        log.info("Received file replication request for fileId: {} from sourceDeviceUrl: {}", request.getFileId(), request.getSourceDeviceUrl());
        try {
            // Robustly extract the actual URL from Markdown or bracketed format, including optional port
            String rawUrl = request.getSourceDeviceUrl();
            String sourceUrl = rawUrl;

            // Try to match Markdown [text](url):port
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\[.*\\]\\((http[^)]+)\\)(:\\d+)?");
            java.util.regex.Matcher matcher = pattern.matcher(sourceUrl);
            if (matcher.matches()) {
                sourceUrl = matcher.group(1); // URL inside ()
                if (matcher.group(2) != null) {
                    sourceUrl += matcher.group(2); // Append port if present
                }
            } else {
                // Fallback: remove brackets and spaces
                sourceUrl = sourceUrl.replaceAll("[\\[\\]\s]", "");
            }

            log.info("Final sanitized sourceDeviceUrl: {}", sourceUrl);
            log.info("[REPLICATION-LISTENER] Incoming ReplicationRequest: fileId={}, fileName={}, deviceId={}, sourceDeviceUrl={}", request.getFileId(), request.getFileName(), request.getDeviceId(), request.getSourceDeviceUrl());
            // Use deviceId and fileName for replication and download from filesystem endpoint
            String deviceId = request.getDeviceId() != null ? request.getDeviceId() : "";

            // --- CRITICAL: Always use the stored (UUID) file name for download, not originalFileName ---
            // Try to fetch file metadata from source device using fileId, to get the correct stored file name (UUID)
            String fileName = request.getFileName();
            String originalFileName = request.getOriginalFileName(); // for traceability
            if (fileName == null || fileName.isEmpty()) {
                // Attempt to fetch metadata from source device
                try {
                    String metadataUrl = sourceUrl + "/api/files/metadata/" + request.getFileId();
                    log.info("[REPLICATION-LISTENER] Fetching metadata from: {}", metadataUrl);
                    org.springframework.http.ResponseEntity<String> metaResp = new org.springframework.web.client.RestTemplate().getForEntity(metadataUrl, String.class);
                    if (metaResp.getStatusCode().is2xxSuccessful() && metaResp.getBody() != null) {
                        com.fasterxml.jackson.databind.JsonNode metaJson = new com.fasterxml.jackson.databind.ObjectMapper().readTree(metaResp.getBody());
                        if (metaJson.has("fileName")) {
                            fileName = metaJson.get("fileName").asText();
                            log.info("[REPLICATION-LISTENER] Got fileName (UUID) from metadata: {}", fileName);
                        }
                        if (metaJson.has("originalFileName")) {
                            originalFileName = metaJson.get("originalFileName").asText();
                            log.info("[REPLICATION-LISTENER] Got originalFileName from metadata: {}", originalFileName);
                        }
                    }
                } catch (Exception ex) {
                    log.warn("[REPLICATION-LISTENER] Could not fetch file metadata from source device: {}", ex.getMessage());
                }
            }
            if (fileName == null || fileName.isEmpty()) {
                log.error("[REPLICATION-LISTENER] fileName could not be determined for replication!");
                return ResponseEntity.status(400).body("fileName could not be determined.");
            }
            log.info("[REPLICATION-LISTENER] Using fileName (UUID): {}, originalFileName: {}", fileName, originalFileName);

            String downloadUrl = sourceUrl + "/api/files/download/" + deviceId + "/" + fileName;
            log.info("[REPLICATION-LISTENER] Attempting to download file from: {}", downloadUrl);

            org.springframework.http.ResponseEntity<byte[]> response = new org.springframework.web.client.RestTemplate().getForEntity(downloadUrl, byte[].class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                java.nio.file.Path replicatedDir = java.nio.file.Paths.get("./data/replicated");
                java.nio.file.Files.createDirectories(replicatedDir);
                java.nio.file.Path targetPath = replicatedDir.resolve(fileName);
                log.info("[REPLICATION-LISTENER] Saving replicated file to: {}", targetPath.toAbsolutePath());
                java.nio.file.Files.write(targetPath, response.getBody());
                log.info("File {} replicated and saved to {}", fileName, targetPath.toAbsolutePath());
                return ResponseEntity.ok("File replicated and saved to: " + targetPath.toAbsolutePath());
            } else {
                log.error("Failed to download file from {}: status {}", downloadUrl, response.getStatusCode());
                // Enhanced: If 404, log detailed info and suggest diagnostics
                if (response.getStatusCodeValue() == 404) {
                    log.error("[REPLICATION-LISTENER] 404 Not Found when attempting to replicate file. Possible causes: file does not exist on source, deviceId or fileName mismatch, or file not yet uploaded. URL: {}", downloadUrl);
                    log.error("[REPLICATION-LISTENER] Diagnostics: deviceId='{}', fileName='{}', originalFileName='{}', sourceUrl='{}'", deviceId, fileName, originalFileName, sourceUrl);
                }
                return ResponseEntity.status(response.getStatusCodeValue()).body("Failed to download file from source device. Status: " + response.getStatusCode() + ". Check logs for diagnostics.");
            }
        } catch (Exception e) {
            log.error("Exception during file replication: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Exception during file replication: " + e.getMessage());
        }
    }
}