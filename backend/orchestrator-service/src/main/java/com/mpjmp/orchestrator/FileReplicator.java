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
            String fileName = filePath.getFileName().toString();
            String deviceId = filePath.getParent().getFileName().toString();

            // Set up ReplicationRequest payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("fileId", fileName); // Assuming fileName is used as fileId, adjust if needed
            payload.put("fileName", fileName);
            payload.put("sourceDeviceUrl", "${SOURCE_DEVICE_URL}"); // TODO: Replace with actual device URL or config property

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
}
