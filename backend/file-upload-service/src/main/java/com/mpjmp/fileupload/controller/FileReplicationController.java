package com.mpjmp.fileupload.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.io.File;

@RestController
@RequestMapping("/api/files")
public class FileReplicationController {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @GetMapping("/download/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileId) {
        // Find the file in the upload directory (recursive search)
        File dir = new File(uploadDir);
        File target = findFileById(dir, fileId);
        if (target == null || !target.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        FileSystemResource resource = new FileSystemResource(target);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + target.getName());
        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }

    private File findFileById(File dir, String fileId) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return null;
        File[] files = dir.listFiles();
        if (files == null) return null;
        for (File file : files) {
            if (file.isDirectory()) {
                File found = findFileById(file, fileId);
                if (found != null) return found;
            } else if (file.getName().startsWith(fileId + "_")) {
                return file;
            }
        }
        return null;
    }
}
