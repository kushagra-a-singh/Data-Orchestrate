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
            // Use only the plain base URL for sourceDeviceUrl (no markdown brackets)
            String rawUrl = request.getSourceDeviceUrl();
            // Remove any markdown or bracketed formatting (e.g., [text](url))
            String sourceUrl = rawUrl.replaceAll("[\\[\\]() ]", "");
            if (sourceUrl.endsWith(",")) {
                sourceUrl = sourceUrl.substring(0, sourceUrl.length() - 1);
            }
            log.info("Final sanitized sourceDeviceUrl: {}", sourceUrl);
            log.info("[REPLICATION-LISTENER] Incoming ReplicationRequest: fileId={}, fileName={}, deviceId={}, sourceDeviceUrl={}", request.getFileId(), request.getFileName(), request.getDeviceId(), request.getSourceDeviceUrl());
            String deviceId = request.getDeviceId() != null ? request.getDeviceId() : "";
            String fileName = request.getFileName();
            String originalFileName = request.getOriginalFileName();
            if (fileName == null || fileName.isEmpty()) {
                log.error("[REPLICATION-LISTENER] fileName must be provided in the replication request!");
                return ResponseEntity.status(400).body("fileName must be provided.");
            }
            log.info("[REPLICATION-LISTENER] Using fileName: {}, originalFileName: {}", fileName, originalFileName);
            // --- FIX: Always use file-upload-service port for download ---
            // Get device info from DeviceConfigUtil
            String fileUploadServiceUrl = null;
            try {
                // Try to get device info by deviceId
                com.example.storage_service.model.DeviceInfo deviceInfo = deviceRepository.findById(deviceId).orElse(null);
                if (deviceInfo != null) {
                    java.util.Map<String, String> deviceMap = com.dataorchestrate.common.DeviceConfigUtil.getDeviceByName(deviceInfo.getDeviceName());
                    if (deviceMap != null && deviceMap.get("ip") != null && deviceMap.get("file_upload_port") != null) {
                        fileUploadServiceUrl = "http://" + deviceMap.get("ip") + ":" + deviceMap.get("file_upload_port");
                    }
                }
            } catch (Exception ex) {
                log.warn("[REPLICATION-LISTENER] Could not resolve file-upload-service URL for deviceId {}: {}", deviceId, ex.getMessage());
            }
            if (fileUploadServiceUrl == null) {
                log.warn("[REPLICATION-LISTENER] Falling back to sourceUrl from request (may be wrong!): {}", sourceUrl);
                fileUploadServiceUrl = sourceUrl;
            }
            String downloadUrl = fileUploadServiceUrl + "/api/files/download/" + deviceId + "/" + fileName;
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