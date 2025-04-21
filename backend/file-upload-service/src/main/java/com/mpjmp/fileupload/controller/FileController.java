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
    public ResponseEntity<FileMetadata> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("uploadedBy") String uploadedBy) throws IOException {
        FileMetadata metadata = fileUploadService.uploadFile(file, uploadedBy, deviceId, getDeviceIp());
        return ResponseEntity.ok(metadata);
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
        // Defensive: decode fileName in case it's URL-encoded (spaces, special chars)
        String decodedFileName = java.net.URLDecoder.decode(fileName, java.nio.charset.StandardCharsets.UTF_8);
        log.info("[DOWNLOAD] Decoded fileName: {} for deviceId: {}", decodedFileName, deviceId);

        // Always use the UUID (stored) fileName for lookup, not originalFileName
        java.nio.file.Path uploadDirPath = java.nio.file.Paths.get(fileUploadService.getAbsoluteUploadDir());
        java.nio.file.Path deviceDirPath = uploadDirPath.resolve(deviceId);

        // First try direct path with the provided filename
        java.nio.file.Path filePath = deviceDirPath.resolve(decodedFileName);
        log.info("[DOWNLOAD] Checking direct file path: {}", filePath.toAbsolutePath());

        boolean foundByDirectPath = java.nio.file.Files.exists(filePath);
        if (!foundByDirectPath) {
            log.warn("[DOWNLOAD] File not found at direct path: {}. Attempting metadata lookup by original filename.", filePath.toAbsolutePath());
            // Try to find the file metadata by original filename
            com.mpjmp.common.model.FileMetadata metadata = fileUploadService.findFileByOriginalName(deviceId, decodedFileName);
            if (metadata != null && metadata.getFileName() != null) {
                filePath = deviceDirPath.resolve(metadata.getFileName());
                log.info("[DOWNLOAD] Found file by metadata, trying path: {}", filePath.toAbsolutePath());
                foundByDirectPath = java.nio.file.Files.exists(filePath);
            }
        }

        if (!foundByDirectPath) {
            // Enhanced: Log all files in device directory for diagnostics
            try {
                java.util.stream.Stream<java.nio.file.Path> files = java.nio.file.Files.list(deviceDirPath);
                StringBuilder sb = new StringBuilder("[DOWNLOAD] Files present in deviceDirPath: ");
                files.forEach(p -> sb.append(p.getFileName()).append(", "));
                log.error(sb.toString());
            } catch (Exception e) {
                log.error("[DOWNLOAD] Could not list files in deviceDirPath: {}", deviceDirPath.toAbsolutePath(), e);
            }
            log.error("[DOWNLOAD] File NOT FOUND for deviceId: {}, fileName: {} (decoded: {}). Checked path: {}. Returning 404.", deviceId, fileName, decodedFileName, filePath.toAbsolutePath());
            return ResponseEntity.status(404).body(null);
        }

        java.io.InputStream inputStream = java.nio.file.Files.newInputStream(filePath);
        org.springframework.core.io.InputStreamResource resource = new org.springframework.core.io.InputStreamResource(inputStream);
        // Always set Content-Disposition to the original file name if possible
        String originalFileName = decodedFileName;
        // Try to get originalFileName from metadata
        com.mpjmp.common.model.FileMetadata metadata = fileUploadService.findFileByOriginalName(deviceId, decodedFileName);
        if (metadata != null && metadata.getOriginalFileName() != null) {
            originalFileName = metadata.getOriginalFileName();
        }
        log.info("[DOWNLOAD] Serving file: {} as {}", filePath.toAbsolutePath(), originalFileName);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + originalFileName + "\"")
                .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
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
            String fileName = (String) replicationRequest.get("fileName");
            String originalFileName = (String) replicationRequest.get("originalFileName");
            String deviceId = (String) replicationRequest.get("deviceId");
            String contentBase64 = (String) replicationRequest.get("content");

            if (fileName == null || deviceId == null || contentBase64 == null) {
                return ResponseEntity.badRequest().body("Missing required fields");
            }

            byte[] fileContent = java.util.Base64.getDecoder().decode(contentBase64);
            Path uploadPath = Paths.get(fileUploadService.getAbsoluteUploadDir(), deviceId);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            Path filePath = uploadPath.resolve(fileName);
            Files.write(filePath, fileContent);
            log.info("[REPLICATION] File received and saved at {} from device {}", filePath.toAbsolutePath(), deviceId);
            // Optionally: Save metadata here if needed
            return ResponseEntity.ok("Replication successful");
        } catch (Exception e) {
            log.error("[REPLICATION] Error replicating file: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Replication failed: " + e.getMessage());
        }
    }

    private String getDeviceIp() {
        // Implement logic to get device IP
        return "localhost"; // Placeholder
    }
}