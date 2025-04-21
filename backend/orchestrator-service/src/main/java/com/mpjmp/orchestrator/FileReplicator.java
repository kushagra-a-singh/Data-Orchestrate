package com.mpjmp.orchestrator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.Files;
import java.util.Base64;
import com.dataorchestrate.common.DeviceConfigUtil;
import com.dataorchestrate.common.NotificationSender;
import jakarta.annotation.PostConstruct;

@Slf4j
@Component
public class FileReplicator {
    @Value("${app.upload.dir}")
    private String uploadDir;

    private final Set<String> replicatedFiles = ConcurrentHashMap.newKeySet();
    private final RestTemplate restTemplate = new RestTemplate();

    private Map<String, String> selfDevice;
    private List<Map<String, String>> peerDevices;

    @PostConstruct
    private void initDeviceConfig() {
        selfDevice = DeviceConfigUtil.getSelfDevice();
        peerDevices = DeviceConfigUtil.getPeerDevices();
        if (selfDevice == null) {
            throw new RuntimeException("Could not identify self device from devices.json");
        }
    }

    private List<String> getPeerDeviceUrls() {
        return peerDevices.stream()
            .map(d -> "http://" + d.get("ip") + ":" + d.get("storage_port"))
            .collect(Collectors.toList());
    }

    // Poll every 10 seconds
    @Scheduled(fixedDelay = 10000)
    public void replicateNewFiles() {
        try {
            Path absUploadDir = Paths.get(uploadDir).toAbsolutePath();
            if (!Files.exists(absUploadDir)) return;
            Files.walk(absUploadDir)
                .filter(Files::isRegularFile)
                .forEach(this::replicateFile);
        } catch (IOException e) {
            log.error("Error scanning upload dir for replication", e);
        }
    }

    private void replicateFile(Path filePath) {
        try {
            String fileKey = filePath.toAbsolutePath().toString();
            if (replicatedFiles.contains(fileKey)) return;
            String fileName = filePath.getFileName().toString(); // UUID file name (e.g., efdbd8a9-26a5-4630-8820-d950c533b25b.pdf)
            String deviceId = filePath.getParent().getFileName().toString();

            NotificationSender.sendNotification(
                "info", "Replication Request", "Replication request received for file: " + fileName, null, null, deviceId, null
            );

            Map<String, Object> payload = new HashMap<>();
            payload.put("fileName", fileName); // Only UUID file name
            payload.put("deviceId", deviceId);
            payload.put("sourceDeviceUrl", getLocalDeviceUrl());

            log.info("[REPLICATION] About to replicate. fileName (UUID): {}, deviceId: {}", fileName, deviceId);
            log.info("[REPLICATION] Payload for {}: {}", fileName, payload);
            log.info("[REPLICATION] Replicating file from path: {} (deviceId: {})", filePath, deviceId);

            NotificationSender.sendNotification(
                "progress", "Replication Started", "Replicating file: " + fileName, 0.0, null, deviceId, null
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> req = new HttpEntity<>(payload, headers);

            for (String peer : getPeerDeviceUrls()) {
                String url = peer + "/replicate-file";
                try {
                    ResponseEntity<String> resp = restTemplate.postForEntity(url, req, String.class);
                    log.info("Replicated file {} to {}: {}", fileName, url, resp.getStatusCode());
                } catch (Exception ex) {
                    log.warn("Failed to replicate file {} to {}: {}", fileName, url, ex.getMessage());
                    NotificationSender.sendNotification(
                        "error", "Replication Failed", "Failed to replicate file: " + fileName + " to " + url + ". Error: " + ex.getMessage(), null, null, deviceId, null
                    );
                }
            }
            replicatedFiles.add(fileKey);
            NotificationSender.sendNotification(
                "success", "Replication Complete", "File replicated and saved at: " + filePath.toString(), 1.0, null, deviceId, filePath.toString()
            );
        } catch (Exception e) {
            log.error("Error replicating file {}", filePath, e);
            NotificationSender.sendNotification(
                "error", "Replication Error", "Error replicating file: " + filePath.getFileName() + ". Error: " + e.getMessage(), null, null, null, filePath.toString()
            );
        }
    }

    // Helper to get this device's base URL (auto-detects IP and port)
    private String getLocalDeviceUrl() {
        return "http://" + selfDevice.get("ip") + ":" + selfDevice.get("storage_port");
    }
}
