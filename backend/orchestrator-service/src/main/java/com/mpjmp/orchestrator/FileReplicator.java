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

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.Files;
import java.util.Base64;

@Slf4j
@Component
public class FileReplicator {
    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${replication.peers}")
    private String peerUrls; // comma-separated list of peer base URLs

    private final Set<String> replicatedFiles = ConcurrentHashMap.newKeySet();
    private final RestTemplate restTemplate = new RestTemplate();

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

            // Set up ReplicationRequest payload (ONLY UUID file name is sent)
            Map<String, Object> payload = new HashMap<>();
            payload.put("fileName", fileName); // Only UUID file name
            payload.put("deviceId", deviceId);
            payload.put("sourceDeviceUrl", getLocalDeviceUrl());

            log.info("[REPLICATION] Payload for {}: {}", fileName, payload);
            log.info("[REPLICATION] Replicating file from path: {} (deviceId: {})", filePath, deviceId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> req = new HttpEntity<>(payload, headers);

            for (String peer : peerUrls.split(",")) {
                String url = peer.trim() + "/replicate-file";
                try {
                    ResponseEntity<String> resp = restTemplate.postForEntity(url, req, String.class);
                    log.info("Replicated file {} to {}: {}", fileName, url, resp.getStatusCode());
                } catch (Exception ex) {
                    log.warn("Failed to replicate file {} to {}: {}", fileName, url, ex.getMessage());
                }
            }
            replicatedFiles.add(fileKey);
        } catch (Exception e) {
            log.error("Error replicating file {}", filePath, e);
        }
    }

    // Helper to get this device's base URL (auto-detects IP and port)
    private String getLocalDeviceUrl() {
        try {
            String ip = java.net.InetAddress.getLocalHost().getHostAddress();
            // Try to get port from server.port property or default to 8080
            String port = System.getProperty("server.port");
            if (port == null || port.isEmpty()) {
                port = System.getenv().getOrDefault("SERVER_PORT", "8080");
            }
            return "http://" + ip + ":" + port;
        } catch (Exception e) {
            log.warn("Could not auto-detect device IP, defaulting to localhost:8080");
            return "http://localhost:8080";
        }
    }
}
