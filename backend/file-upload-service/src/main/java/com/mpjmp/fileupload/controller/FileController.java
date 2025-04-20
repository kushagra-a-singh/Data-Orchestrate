package com.mpjmp.fileupload.controller;

import com.mpjmp.fileupload.model.FileMetadata;
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
    public ResponseEntity<InputStreamResource> downloadFileByDevice(
            @PathVariable String deviceId,
            @PathVariable String fileName) throws IOException {
        // Log received path variables for debugging
        log.info("[DOWNLOAD] deviceId received: {}", deviceId);
        log.info("[DOWNLOAD] fileName received: {}", fileName);
        // Defensive: decode fileName in case it's URL-encoded (spaces, special chars)
        String decodedFileName = URLDecoder.decode(fileName, java.nio.charset.StandardCharsets.UTF_8);
        log.info("[DOWNLOAD] Decoded fileName: {}", decodedFileName);
        Path filePath = Paths.get(fileUploadService.getAbsoluteUploadDir()).resolve(deviceId).resolve(decodedFileName);
        log.info("[DOWNLOAD] Resolved filePath: {}", filePath.toAbsolutePath());
        if (!Files.exists(filePath)) {
            log.warn("[DOWNLOAD] File not found at path: {}", filePath.toAbsolutePath());
            return ResponseEntity.notFound().build();
        }
        InputStream inputStream = Files.newInputStream(filePath);
        InputStreamResource resource = new InputStreamResource(inputStream);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + decodedFileName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
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

    private String getDeviceIp() {
        // Implement logic to get device IP
        return "localhost"; // Placeholder
    }
} 