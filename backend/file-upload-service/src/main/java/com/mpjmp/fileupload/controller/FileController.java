package com.mpjmp.fileupload.controller;

import com.mpjmp.common.model.FileMetadata;
import com.mpjmp.fileupload.service.FileUploadService;
import com.dataorchestrate.common.DeviceIdentifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileUploadService fileUploadService;

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${app.device.id}")
    private String deviceId;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("uploadedBy") String uploadedBy,
            @RequestParam(value = "deviceId", required = false) String deviceId) throws IOException {
        FileMetadata metadata = fileUploadService.uploadFile(file, uploadedBy, deviceId, getDeviceIp());
        Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("fileId", metadata.getId());
        resp.put("fileName", metadata.getFileName());
        resp.put("fileType", metadata.getFileType());
        resp.put("fileSize", metadata.getFileSize());
        resp.put("uploadPath", metadata.getUploadPath());
        resp.put("processedPath", metadata.getProcessedPath());
        resp.put("storagePath", metadata.getStoragePath());
        resp.put("uploadTime", metadata.getUploadTime());
        resp.put("processedTime", metadata.getProcessedTime());
        resp.put("deviceId", metadata.getDeviceId());
        resp.put("deviceName", metadata.getDeviceName());
        resp.put("status", metadata.getStatus());
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileId) throws IOException {
        FileMetadata metadata = fileUploadService.getFileMetadata(fileId);
        if (metadata == null) {
            return ResponseEntity.notFound().build();
        }

        Path filePath = Paths.get(uploadDir, metadata.getFileName());
        byte[] fileContent = Files.readAllBytes(filePath);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + metadata.getOriginalFileName() + "\"")
                .body(new ByteArrayResource(fileContent));
    }

    @PostMapping("/sync")
    public ResponseEntity<Void> syncFile(@RequestBody Map<String, Object> syncRequest) throws IOException {
        String fileId = (String) syncRequest.get("fileId");
        String fileName = (String) syncRequest.get("fileName");
        byte[] content = (byte[]) syncRequest.get("content");
        long version = ((Number) syncRequest.get("version")).longValue();

        // Save synced file
        Path filePath = Paths.get(uploadDir, fileName);
        Files.write(filePath, content);

        // Update metadata
        FileMetadata metadata = fileUploadService.getFileMetadata(fileId);
        if (metadata != null) {
            metadata.setVersion(version);
            fileUploadService.updateFileMetadata(metadata);
        }

        return ResponseEntity.ok().build();
    }

    // --- NEW ENDPOINT for SYNC ---
    @PostMapping("/sync-device")
    public ResponseEntity<Void> syncFileFromDevice(@RequestBody Map<String, Object> syncRequest) throws IOException {
        String fileName = (String) syncRequest.get("fileName");
        String deviceId = (String) syncRequest.get("deviceId");
        String originalFileName = (String) syncRequest.get("originalFileName");
        String contentType = (String) syncRequest.get("contentType");
        byte[] content = java.util.Base64.getDecoder().decode((String) syncRequest.get("content"));
        Path uploadPath = Paths.get(fileUploadService.getAbsoluteUploadDir(), deviceId);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        Path filePath = uploadPath.resolve(fileName);
        Files.write(filePath, content);
        log.info("[SYNC] File received and saved at {}", filePath.toAbsolutePath());
        // Optionally: Save metadata here if needed
        return ResponseEntity.ok().build();
    }

    // Download file by deviceId and fileName from local filesystem (for replication)
    @GetMapping("/download/{deviceId}/{fileName}")
    public ResponseEntity<Resource> downloadFileByDevice(
            @PathVariable String deviceId,
            @PathVariable String fileName) throws IOException {
        Path filePath = Paths.get(uploadDir, deviceId, fileName);
        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }
        byte[] fileContent = Files.readAllBytes(filePath);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(new ByteArrayResource(fileContent));
    }

    @GetMapping("/device/id")
    public ResponseEntity<Map<String, String>> getDeviceId() {
        DeviceIdentifier deviceIdentifier = new DeviceIdentifier();
        String realDeviceId = deviceIdentifier.getDeviceId();
        Map<String, String> resp = new java.util.HashMap<>();
        resp.put("deviceId", realDeviceId);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/list")
    public ResponseEntity<List<FileMetadata>> listFiles(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String uploadedBy) {
        List<FileMetadata> files = fileUploadService.listFiles(status, uploadedBy);
        return ResponseEntity.ok(files);
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteFile(@PathVariable String fileId) {
        boolean deleted = fileUploadService.deleteFile(fileId);
        return deleted ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    // --- API endpoint to fetch metadata for a fileId (for orchestrator replication logic) ---
    @GetMapping("/metadata/{fileId}")
    public ResponseEntity<FileMetadata> getFileMetadataById(@PathVariable String fileId) {
        FileMetadata metadata = fileUploadService.getFileMetadata(fileId);
        if (metadata == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(metadata);
    }

    // --- REPLICATION ENDPOINT ---
    @PostMapping("/replicate-file")
    public ResponseEntity<String> replicateFile(@RequestBody Map<String, Object> replicationRequest) {
        try {
            String fileId = (String) replicationRequest.get("fileId");
            String fileName = (String) replicationRequest.get("fileName");
            String sourceDeviceUrl = (String) replicationRequest.get("sourceDeviceUrl");
            String deviceId = (String) replicationRequest.get("deviceId");
            // Download file from source device
            java.net.URL url = new java.net.URL(sourceDeviceUrl + "/api/files/download/" + deviceId + "/" + fileName);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            try (java.io.InputStream is = conn.getInputStream()) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
            }
            Path replicatedPath = Paths.get(uploadDir, "replicated", fileName);
            if (!Files.exists(replicatedPath.getParent())) {
                Files.createDirectories(replicatedPath.getParent());
            }
            Files.write(replicatedPath, baos.toByteArray());
            return ResponseEntity.ok("File replicated and saved to: " + replicatedPath.toAbsolutePath());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Replication failed: " + e.getMessage());
        }
    }

    private String getDeviceIp() {
        // Implement logic to get device IP
        return "localhost"; // Placeholder
    }
}